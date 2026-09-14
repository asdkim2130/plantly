package project.plantly.companyTest.companyServiceTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.plantly.companyTest.support.CompanyCreateRequestBuilder;
import project.plantly.domain.company.dto.MyCompanyCreateRequest;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.service.CompanyRegistrationService;
import project.plantly.domain.company.service.CompanyService;
import project.plantly.domain.company.service.CompanyVerificationService;
import project.plantly.global.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// CompanyRegistrationService 는 순서만 잡는 얇은 빈이라, 검증할 것도 그 순서 하나다.
// 다만 그 순서가 이 빈의 존재 이유다 — 인증 갱신(국세청 HTTP)이 등록 트랜잭션보다 먼저,
// 그리고 밖에서 끝나야 커넥션을 붙잡은 채 외부 응답을 기다리지 않는다.
@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyRegistrationService: 자가등록 오케스트레이션")
class CompanyRegistrationServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long VERIFICATION_ID = 99L;

    @Mock CompanyVerificationService verificationService;
    @Mock CompanyService companyService;

    @InjectMocks CompanyRegistrationService service;

    private final MyCompanyCreateRequest request =
            CompanyCreateRequestBuilder.aRequest().buildMy(VERIFICATION_ID);

    @Test
    @DisplayName("인증 갱신을 먼저 끝낸 뒤에 등록한다 — 국세청 호출이 등록 트랜잭션 안으로 들어가지 않게")
    void refreshesVerificationBeforeCreating() {
        given(companyService.createByUser(eq(USER_ID), any(MyCompanyCreateRequest.class))).willReturn(10L);

        Long id = service.register(USER_ID, request);

        assertThat(id).isEqualTo(10L);
        InOrder order = inOrder(verificationService, companyService);
        order.verify(verificationService).refreshIfExpired(USER_ID, VERIFICATION_ID);
        order.verify(companyService).createByUser(USER_ID, request);
    }

    @Test
    @DisplayName("갱신이 실패하면 등록 트랜잭션은 시작조차 하지 않는다")
    void failedRefresh_doesNotStartRegistration() {
        willThrow(new BusinessException(CompanyErrorCode.VERIFICATION_CLOSED))
                .given(verificationService).refreshIfExpired(USER_ID, VERIFICATION_ID);

        assertThatThrownBy(() -> service.register(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.VERIFICATION_CLOSED);

        verify(companyService, never()).createByUser(any(), any());
    }
}
