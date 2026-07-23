package project.plantly.companyTest.companyServiceTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import project.plantly.companyTest.support.CompanyCreateRequestBuilder;
import project.plantly.companyTest.support.CompanyVerificationFixture;
import project.plantly.domain.company.dto.CompanyDraftResponse;
import project.plantly.domain.company.dto.MyCompanyCreateRequest;
import project.plantly.domain.company.entity.CompanyDraft;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.repository.CompanyDraftRepository;
import project.plantly.domain.company.repository.CompanyVerificationRepository;
import project.plantly.domain.company.service.CompanyDraftService;
import project.plantly.global.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// CompanyDraftService 단위 테스트: 초안 저장/조회/폐기의 "분기와 방어"를 못 박는다.
//  - 리포지토리는 mock, ObjectMapper 는 진짜를 주입한다 — 직렬화가 실제로 돌아 저장 payload 를 검증할 수 있고,
//    조회는 저장 문자열을 그대로 역직렬화하는 왕복까지 확인된다(record equals 로 동치 비교).
//  - upsert(신규 save vs 기존 updatePayload), CONSUMED 저장 거절, 매 요청 소유권 검사(크로스키 방어),
//    404/409 에러코드 매핑을 확인한다. (초안은 저장 시 값 검증을 하지 않는 "느슨한 저장"이라 그 경계도 함께 못 박는다.)
@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyDraftService: 임시저장 초안")
class CompanyDraftServiceTest {

    @Mock CompanyDraftRepository draftRepository;
    @Mock CompanyVerificationRepository verificationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CompanyDraftService service;

    private static final Long USER_ID = 7L;
    private static final Long VERIFICATION_ID = 99L;

    // 관심 필드만 채운 자가등록 폼(부분 입력). 초안 payload 로 직렬화되는 원본이다.
    private final MyCompanyCreateRequest payload = CompanyCreateRequestBuilder.aRequest()
            .videoUrl("https://youtu.be/demo")
            .brandColor("#123456")
            .categoryIds(List.of(1L, 2L))
            .buildMy(VERIFICATION_ID);

    @BeforeEach
    void setUp() {
        service = new CompanyDraftService(draftRepository, verificationRepository, objectMapper);
    }

    // 본인이 받은, 사용 가능한(미소비) 인증이 조회된다고 가정한다.
    private void givenUsableVerification() {
        given(verificationRepository.findByIdAndUserId(VERIFICATION_ID, USER_ID))
                .willReturn(Optional.of(CompanyVerificationFixture.usable(VERIFICATION_ID, USER_ID)));
    }

    private String serialized(MyCompanyCreateRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Nested
    @DisplayName("save (자동저장 upsert)")
    class Save {

        @Test
        @DisplayName("초안이 없으면 새 CompanyDraft 를 verificationId·userId·직렬화 payload 로 저장한다")
        void newDraft_isCreated() {
            givenUsableVerification();
            given(draftRepository.findByVerificationId(VERIFICATION_ID)).willReturn(Optional.empty());

            service.save(USER_ID, VERIFICATION_ID, payload);

            ArgumentCaptor<CompanyDraft> captor = ArgumentCaptor.forClass(CompanyDraft.class);
            verify(draftRepository).save(captor.capture());
            CompanyDraft saved = captor.getValue();
            assertThat(saved.getVerificationId()).isEqualTo(VERIFICATION_ID);
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getPayload()).isEqualTo(serialized(payload));
        }

        @Test
        @DisplayName("초안이 이미 있으면 새로 저장하지 않고 기존 payload 를 통째 교체한다 (인증당 1행 유지)")
        void existingDraft_isReplacedInPlace() {
            givenUsableVerification();
            CompanyDraft existing = CompanyDraft.create(VERIFICATION_ID, USER_ID, "{\"old\":true}");
            given(draftRepository.findByVerificationId(VERIFICATION_ID)).willReturn(Optional.of(existing));

            service.save(USER_ID, VERIFICATION_ID, payload);

            assertThat(existing.getPayload()).isEqualTo(serialized(payload));
            verify(draftRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 등록에 소비된(CONSUMED) 인증에는 저장을 거절한다 (409 VERIFICATION_ALREADY_USED)")
        void consumedVerification_isRejected() {
            given(verificationRepository.findByIdAndUserId(VERIFICATION_ID, USER_ID))
                    .willReturn(Optional.of(CompanyVerificationFixture.consumed(VERIFICATION_ID, USER_ID, 55L)));

            assertThatThrownBy(() -> service.save(USER_ID, VERIFICATION_ID, payload))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.VERIFICATION_ALREADY_USED);

            verify(draftRepository, never()).findByVerificationId(any());
            verify(draftRepository, never()).save(any());
        }

        @Test
        @DisplayName("남의(혹은 없는) 인증 식별자로는 저장할 수 없다 (400 VERIFICATION_NOT_FOUND)")
        void foreignVerification_isRejected() {
            given(verificationRepository.findByIdAndUserId(VERIFICATION_ID, USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.save(USER_ID, VERIFICATION_ID, payload))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.VERIFICATION_NOT_FOUND);

            verify(draftRepository, never()).save(any());
        }

        // 계약 경계: 저장은 값 검증을 하지 않는다. 구조적으로만 막는 것은 CONSUMED 뿐이라,
        // 만료된 인증이어도 작성 중 폼은 계속 저장된다(필수값·만료 검증은 발행 시점에서만).
        @Test
        @DisplayName("만료된 인증이어도 저장은 허용된다 (막는 것은 CONSUMED 뿐 — 느슨한 저장)")
        void expiredVerification_isStillAllowed() {
            given(verificationRepository.findByIdAndUserId(VERIFICATION_ID, USER_ID))
                    .willReturn(Optional.of(CompanyVerificationFixture.expired(VERIFICATION_ID, USER_ID)));
            given(draftRepository.findByVerificationId(VERIFICATION_ID)).willReturn(Optional.empty());

            service.save(USER_ID, VERIFICATION_ID, payload);

            verify(draftRepository).save(any(CompanyDraft.class));
        }
    }

    @Nested
    @DisplayName("get (재진입 폼 복원)")
    class Get {

        @Test
        @DisplayName("저장된 초안을 역직렬화한 payload 와 마지막 저장 시각을 돌려준다 (직렬화 왕복)")
        void returnsDeserializedPayloadAndTimestamp() {
            givenUsableVerification();
            LocalDateTime updatedAt = LocalDateTime.of(2026, 7, 23, 10, 0);
            CompanyDraft draft = CompanyDraft.create(VERIFICATION_ID, USER_ID, serialized(payload));
            ReflectionTestUtils.setField(draft, "updatedAt", updatedAt); // @UpdateTimestamp 는 영속 시 채워지므로 심어준다
            given(draftRepository.findByVerificationId(VERIFICATION_ID)).willReturn(Optional.of(draft));

            CompanyDraftResponse response = service.get(USER_ID, VERIFICATION_ID);

            assertThat(response.payload()).isEqualTo(payload); // record equals: 저장→복원 왕복이 원본과 동일
            assertThat(response.updatedAt()).isEqualTo(updatedAt);
        }

        @Test
        @DisplayName("저장된 초안이 없으면 404 (DRAFT_NOT_FOUND)")
        void noDraft_isNotFound() {
            givenUsableVerification();
            given(draftRepository.findByVerificationId(VERIFICATION_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(USER_ID, VERIFICATION_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.DRAFT_NOT_FOUND);
        }

        @Test
        @DisplayName("남의 인증으로는 조회할 수 없다 — 소유권 검사가 초안 조회보다 먼저 걸린다")
        void foreignVerification_isRejectedBeforeDraftLookup() {
            given(verificationRepository.findByIdAndUserId(VERIFICATION_ID, USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(USER_ID, VERIFICATION_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.VERIFICATION_NOT_FOUND);

            verify(draftRepository, never()).findByVerificationId(any());
        }
    }

    @Nested
    @DisplayName("delete (수동 폐기)")
    class Delete {

        @Test
        @DisplayName("본인 인증이면 초안을 삭제한다")
        void ownedVerification_deletesDraft() {
            givenUsableVerification();

            service.delete(USER_ID, VERIFICATION_ID);

            verify(draftRepository).deleteByVerificationId(VERIFICATION_ID);
        }

        @Test
        @DisplayName("남의 인증으로는 폐기할 수 없고, 삭제도 시도하지 않는다 (400 VERIFICATION_NOT_FOUND)")
        void foreignVerification_isRejected() {
            given(verificationRepository.findByIdAndUserId(VERIFICATION_ID, USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(USER_ID, VERIFICATION_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.VERIFICATION_NOT_FOUND);

            verify(draftRepository, never()).deleteByVerificationId(any());
        }
    }
}
