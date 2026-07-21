package project.plantly.companyTest.companyServiceTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.plantly.companyTest.support.CompanyVerificationFixture;
import project.plantly.domain.company.dto.CompanyVerificationRequest;
import project.plantly.domain.company.dto.CompanyVerificationResponse;
import project.plantly.domain.company.enums.VerificationOutcome;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.nts.NtsClient;
import project.plantly.domain.company.service.CompanyVerificationService;
import project.plantly.domain.company.service.CompanyVerificationWriter;
import project.plantly.global.exception.BusinessException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// CompanyVerificationService 단위 테스트.
// 관심사는 "국세청 판정을 우리 도메인 결과로 어떻게 옮기는가" — 판정별 에러 매핑, 재시도 제한, 감사 로그 기록 여부.
// 국세청 응답 파싱 자체는 NtsRestClientTest 가, DB 접근은 Writer 가 담당한다.
@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyVerificationService: 선행 사업자 인증")
class CompanyVerificationServiceTest {

    private static final Long USER_ID = 7L;

    @Mock NtsClient ntsClient;
    @Mock CompanyVerificationWriter writer;

    @InjectMocks CompanyVerificationService service;

    private final CompanyVerificationRequest request =
            new CompanyVerificationRequest("123-45-67890", "홍길동", LocalDate.of(2020, 1, 2));

    private void givenNtsReturns(VerificationOutcome outcome) {
        given(ntsClient.verify(any(), any(), any()))
                .willReturn(new NtsClient.Result(outcome, "{\"raw\":true}"));
    }

    @Test
    @DisplayName("국세청 판정을 통과하면 정규화된 사업자번호로 인증을 발급한다")
    void verify_success_issuesVerification() {
        givenNtsReturns(VerificationOutcome.VALID);
        given(writer.issue(eq(USER_ID), eq("1234567890"), eq("홍길동"), any(), any(), any(), any()))
                .willReturn(CompanyVerificationFixture.usable(99L, USER_ID));

        CompanyVerificationResponse response = service.verify(USER_ID, request);

        assertThat(response.verificationId()).isEqualTo(99L);
        // 하이픈이 제거된 형태로 국세청에 질의하고 저장한다 — 표기가 섞이면 활성 유니크가 중복을 못 막는다.
        verify(ntsClient).verify(eq("1234567890"), eq("홍길동"), eq(LocalDate.of(2020, 1, 2)));
    }

    @Test
    @DisplayName("사업자번호 형식이 틀리면 국세청을 호출하지 않고 거른다")
    void verify_invalidFormat_doesNotCallNts() {
        CompanyVerificationRequest malformed =
                new CompanyVerificationRequest("12345", "홍길동", LocalDate.of(2020, 1, 2));

        assertThatThrownBy(() -> service.verify(USER_ID, malformed))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.BUSINESS_NUMBER_INVALID_FORMAT);

        // 오타로 일일 쿼터를 태우지 않는다.
        verify(ntsClient, never()).verify(any(), any(), any());
    }

    @Nested
    @DisplayName("국세청 판정별 응답")
    class OutcomeMapping {

        @Test
        @DisplayName("진위 불일치는 400(MISMATCH)이며 시도를 감사 로그에 남긴다")
        void mismatch() {
            givenNtsReturns(VerificationOutcome.MISMATCH);

            assertThatThrownBy(() -> service.verify(USER_ID, request))
                    .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.VERIFICATION_MISMATCH);

            verify(writer).recordAttempt(eq(USER_ID), eq("1234567890"), eq("홍길동"), any(),
                    eq(VerificationOutcome.MISMATCH), any());
        }

        @Test
        @DisplayName("미등록 번호는 400(NOT_REGISTERED) — 입력을 고쳐도 소용없는 경우라 불일치와 구분한다")
        void notRegistered() {
            givenNtsReturns(VerificationOutcome.NOT_REGISTERED);

            assertThatThrownBy(() -> service.verify(USER_ID, request))
                    .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.VERIFICATION_NOT_REGISTERED);
        }

        @Test
        @DisplayName("폐업자는 인증을 통과하지 못한다 — 진위확인만으로는 못 거르는 케이스")
        void closed() {
            givenNtsReturns(VerificationOutcome.CLOSED);

            assertThatThrownBy(() -> service.verify(USER_ID, request))
                    .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.VERIFICATION_CLOSED);

            verify(writer, never()).issue(anyLong(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("휴업자도 인증을 통과하지 못한다")
        void suspended() {
            givenNtsReturns(VerificationOutcome.SUSPENDED);

            assertThatThrownBy(() -> service.verify(USER_ID, request))
                    .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.VERIFICATION_SUSPENDED);
        }
    }

    @Test
    @DisplayName("국세청 장애는 503 으로 안내하고, 사용자 잘못이 아니므로 UNAVAILABLE 로 기록한다")
    void verify_ntsDown_mapsToServiceUnavailable() {
        willThrow(new NtsClient.UnavailableException("timeout")).given(ntsClient).verify(any(), any(), any());

        assertThatThrownBy(() -> service.verify(USER_ID, request))
                .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.VERIFICATION_UNAVAILABLE);

        // 재시도 제한 집계에서 제외되는 outcome 으로 남겨야 장애 시간에 하루치 기회를 잃지 않는다.
        verify(writer).recordAttempt(eq(USER_ID), any(), any(), any(),
                eq(VerificationOutcome.UNAVAILABLE), eq(null));
    }

    @Test
    @DisplayName("일일 시도 한도(5회)를 채웠으면 국세청을 호출하지 않고 429 를 반환한다")
    void verify_dailyLimitExceeded() {
        given(writer.countTodayAttempts(eq(USER_ID), any())).willReturn(5L);

        assertThatThrownBy(() -> service.verify(USER_ID, request))
                .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.VERIFICATION_LIMIT_EXCEEDED);

        verify(ntsClient, never()).verify(any(), any(), any());
    }

    @Test
    @DisplayName("한도 미만이면 정상적으로 국세청을 호출한다 (경계값: 4회 사용)")
    void verify_underDailyLimit_proceeds() {
        given(writer.countTodayAttempts(eq(USER_ID), any())).willReturn(4L);
        givenNtsReturns(VerificationOutcome.VALID);
        given(writer.issue(anyLong(), any(), any(), any(), any(), any(), any()))
                .willReturn(CompanyVerificationFixture.usable(99L, USER_ID));

        service.verify(USER_ID, request);

        verify(ntsClient).verify(any(), any(), any());
    }

    @Test
    @DisplayName("발급 시 TTL 30분이 적용된다")
    void verify_appliesThirtyMinuteTtl() {
        givenNtsReturns(VerificationOutcome.VALID);
        given(writer.issue(anyLong(), any(), any(), any(), any(), any(), eq(Duration.ofMinutes(30))))
                .willReturn(CompanyVerificationFixture.usable(99L, USER_ID));

        service.verify(USER_ID, request);

        verify(writer).issue(eq(USER_ID), eq("1234567890"), eq("홍길동"), eq(LocalDate.of(2020, 1, 2)),
                any(), any(LocalDateTime.class), eq(Duration.ofMinutes(30)));
    }
}
