package project.plantly.companyTest.companyServiceTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import project.plantly.companyTest.support.CompanyCreateRequestBuilder;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.entity.link.CompanyMember;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.enums.MemberRole;
import project.plantly.domain.company.enums.SubscriptionStatus;
import project.plantly.domain.company.policy.CompanyRegistrationPolicy;
import project.plantly.domain.company.repository.CompanyMemberRepository;
import project.plantly.domain.company.repository.CompanyRepository;
import project.plantly.domain.company.repository.CompanySubscriptionRepository;
import project.plantly.domain.company.search.CompanySearchDocumentWriter;
import project.plantly.domain.company.service.CompanyChildWriter;
import project.plantly.domain.company.service.CompanyLinkWriter;
import project.plantly.domain.company.service.CompanyService;
import project.plantly.global.exception.BusinessException;
import project.plantly.global.exception.ErrorCode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// CompanyService 단위 테스트: 등급별 outcome 이 아니라 "오케스트레이션/구조"를 검증한다.
//  - 정책은 mock 으로 두고 "전부 호출되는가 / 어떤 구독(subscription)으로 호출되는가" 만 본다. (등급 판정은 정책 단위 테스트 담당)
//  - 등록 경로별 차이(OWNER 멤버 생성, 구독 등급/면제)를 못 박는다.
@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyService: 회사 등록 오케스트레이션")
class CompanyServiceTest {

    @Mock CompanyRepository companyRepository;
    @Mock CompanyChildWriter childWriter;
    @Mock CompanyLinkWriter linkWriter;
    @Mock CompanyMemberRepository companyMemberRepository;
    @Mock CompanySubscriptionRepository companySubscriptionRepository;
    @Mock CompanySearchDocumentWriter searchDocumentWriter;

    private final CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().build();

    // 정책 리스트는 테스트마다 다르므로 생성자 직접 호출로 주입한다. (Mockito 가 List<인터페이스> mock 을 자동 주입하지 못함)
    private CompanyService service(CompanyRegistrationPolicy... policies) {
        return new CompanyService(companyRepository, childWriter, linkWriter, companyMemberRepository,
                companySubscriptionRepository, searchDocumentWriter, List.of(policies));
    }

    // companyRepository.save 가 INSERT 후 id 를 채우는 것을 흉내낸다. (persist 가 직후 company.getId() 를 읽음)
    private void givenSaveAssignsId(long id) {
        given(companyRepository.save(any(Company.class))).willAnswer(inv -> {
            Company c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", id);
            return c;
        });
    }

    @Test
    @DisplayName("유저 자가등록: 본체/구독/자식/링크 저장 후 등록자를 OWNER 멤버로 기록하고 회사 id 를 반환한다")
    void createByUser_savesCompanyAndOwnerMember() {
        givenSaveAssignsId(10L);
        CompanyService service = service();

        Long companyId = service.createByUser(7L, request);

        assertThat(companyId).isEqualTo(10L);
        verify(companyRepository).save(any(Company.class));
        verify(childWriter).write(any(Company.class), eq(request));
        verify(linkWriter).write(any(Company.class), eq(request));
        verify(searchDocumentWriter).write(10L);

        // 구독은 저장된 회사 id 로 연결(1:1)되어 저장된다.
        ArgumentCaptor<CompanySubscription> subCaptor = ArgumentCaptor.forClass(CompanySubscription.class);
        verify(companySubscriptionRepository).save(subCaptor.capture());
        assertThat(subCaptor.getValue().getCompanyId()).isEqualTo(10L);

        ArgumentCaptor<CompanyMember> memberCaptor = ArgumentCaptor.forClass(CompanyMember.class);
        verify(companyMemberRepository).save(memberCaptor.capture());
        CompanyMember member = memberCaptor.getValue();
        assertThat(member.getCompanyId()).isEqualTo(10L);
        assertThat(member.getUserId()).isEqualTo(7L);
        assertThat(member.getRole()).isEqualTo(MemberRole.OWNER);
    }

    @Test
    @DisplayName("유저 자가등록: 주입된 모든 정책을 FREE 구독으로 실행한다")
    void createByUser_appliesAllPoliciesWithFreeSubscription() {
        givenSaveAssignsId(10L);
        CompanyRegistrationPolicy policyA = mock(CompanyRegistrationPolicy.class);
        CompanyRegistrationPolicy policyB = mock(CompanyRegistrationPolicy.class);
        CompanyService service = service(policyA, policyB);

        service.createByUser(7L, request);

        ArgumentCaptor<CompanySubscription> sub = ArgumentCaptor.forClass(CompanySubscription.class);
        verify(policyA).apply(any(Company.class), eq(request), sub.capture());
        verify(policyB).apply(any(Company.class), eq(request), any(CompanySubscription.class));
        assertThat(sub.getValue().effectiveGrade()).isEqualTo(CompanyGrade.FREE);
        assertThat(sub.getValue().isExempt()).isFalse();
    }

    @Test
    @DisplayName("관리자 등록: 본체/구독/자식/링크는 저장하지만 멤버는 만들지 않으며 면제(ADMIN_EXEMPT) 구독으로 정책을 실행한다")
    void createByAdmin_savesCompanyButNoMember() {
        givenSaveAssignsId(20L);
        CompanyRegistrationPolicy policy = mock(CompanyRegistrationPolicy.class);
        CompanyService service = service(policy);

        Long companyId = service.createByAdmin(3L, request);

        assertThat(companyId).isEqualTo(20L);
        verify(companyRepository).save(any(Company.class));
        verify(companySubscriptionRepository).save(any(CompanySubscription.class));
        verify(childWriter).write(any(Company.class), eq(request));
        verify(linkWriter).write(any(Company.class), eq(request));
        verify(searchDocumentWriter).write(20L);
        verify(companyMemberRepository, never()).save(any());

        ArgumentCaptor<CompanySubscription> sub = ArgumentCaptor.forClass(CompanySubscription.class);
        verify(policy).apply(any(Company.class), eq(request), sub.capture());
        assertThat(sub.getValue().isExempt()).isTrue();
        assertThat(sub.getValue().getStatus()).isEqualTo(SubscriptionStatus.ADMIN_EXEMPT);
    }

    @Test
    @DisplayName("정책이 위반을 던지면 저장 이전이라 아무것도 영속화되지 않는다")
    void createByUser_policyThrows_nothingPersisted() {
        CompanyRegistrationPolicy failing = mock(CompanyRegistrationPolicy.class);
        org.mockito.BDDMockito.willThrow(new BusinessException(TestError.FAIL))
                .given(failing).apply(any(Company.class), any(CompanyCreateRequest.class), any(CompanySubscription.class));
        CompanyService service = service(failing);

        assertThatThrownBy(() -> service.createByUser(7L, request))
                .isInstanceOf(BusinessException.class);

        verify(companyRepository, never()).save(any());
        verify(companySubscriptionRepository, never()).save(any());
        verify(childWriter, never()).write(any(), any());
        verify(linkWriter, never()).write(any(), any());
        verify(searchDocumentWriter, never()).write(any());
        verify(companyMemberRepository, never()).save(any());
    }

    // 정책 실패 전파 검증용 임의 에러코드. (실제 정책의 구체 코드는 정책 단위 테스트가 검증)
    private enum TestError implements ErrorCode {
        FAIL;

        @Override
        public org.springframework.http.HttpStatus getStatus() {
            return org.springframework.http.HttpStatus.BAD_REQUEST;
        }

        @Override
        public String getMessage() {
            return "정책 위반";
        }
    }
}
