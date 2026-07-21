package project.plantly.companyTest.companyServiceTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.plantly.companyTest.support.CompanyFixture;
import project.plantly.domain.company.dto.CompanyUpdateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.CompanyMutationPolicy;
import project.plantly.domain.company.policy.GradePolicyRegistry;
import project.plantly.domain.company.policy.rule.BrandColorPolicy;
import project.plantly.domain.company.policy.rule.CategoryLimitPolicy;
import project.plantly.domain.company.policy.rule.DetailImageLimitPolicy;
import project.plantly.domain.company.policy.rule.ReferenceImagePolicy;
import project.plantly.domain.company.policy.rule.VideoUrlPolicy;
import project.plantly.domain.company.repository.CompanyMemberRepository;
import project.plantly.domain.company.repository.CompanyRepository;
import project.plantly.domain.company.repository.CompanySubscriptionRepository;
import project.plantly.domain.company.repository.CompanyVerificationRepository;
import project.plantly.companyTest.support.CompanyVerificationFixture;
import project.plantly.domain.company.entity.CompanyVerification;
import project.plantly.domain.company.enums.VerificationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import project.plantly.domain.company.search.CompanySearchDocumentWriter;
import project.plantly.domain.company.service.CompanyChildWriter;
import project.plantly.domain.company.service.CompanyLinkWriter;
import project.plantly.domain.company.service.CompanyUpdateService;
import project.plantly.global.exception.BusinessException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// CompanyUpdateService 단위 테스트: 수정 경로가 등급 정책(CompanyMutationPolicy)을 재실행해 등급 한도 우회를 막는지 본다.
//  - 정책/등급레지스트리는 진짜(mock 아님)로 주입해 '실제 강제'를 서비스 레벨에서 검증한다. (DB·writer 만 mock)
//  - delta 검증: 이번에 바뀐 컬렉션/필드에 해당하는 정책만 발화한다.
//  - 한도는 '회사의 구독'이 정한다(유저/관리자 동일). ADMIN_EXEMPT 회사는 면제된다.
@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyUpdateService: 수정 경로 등급 정책 재실행")
class CompanyUpdateServiceTest {

    @Mock CompanyRepository companyRepository;
    @Mock CompanyMemberRepository companyMemberRepository;
    @Mock CompanySubscriptionRepository companySubscriptionRepository;
    @Mock CompanyChildWriter childWriter;
    @Mock CompanyLinkWriter linkWriter;
    @Mock CompanySearchDocumentWriter searchDocumentWriter;
    @Mock CompanyVerificationRepository verificationRepository;

    private static final long COMPANY_ID = 1L;
    private static final long OWNER_ID = 7L;
    private static final String DEFAULT_BRAND_COLOR = "#808080";

    private CompanyUpdateService service;

    @BeforeEach
    void setUp() {
        GradePolicyRegistry registry = new GradePolicyRegistry();
        List<CompanyMutationPolicy> mutationPolicies = List.of(
                new CategoryLimitPolicy(registry),
                new DetailImageLimitPolicy(registry),
                new ReferenceImagePolicy(registry),
                new VideoUrlPolicy(registry),
                new BrandColorPolicy(registry));
        service = new CompanyUpdateService(companyRepository, companyMemberRepository, companySubscriptionRepository,
                verificationRepository, childWriter, linkWriter, searchDocumentWriter, mutationPolicies);
    }

    // 소유자 경로: 회사 로드 + 멤버(소유) 확인 + 회사 구독을 스텁하고, 대상 Company 를 돌려준다.
    private Company givenOwnedCompany(CompanySubscription subscription) {
        Company company = CompanyFixture.userCompany();
        given(companyRepository.findById(COMPANY_ID)).willReturn(Optional.of(company));
        given(companyMemberRepository.existsByCompanyIdAndUserId(COMPANY_ID, OWNER_ID)).willReturn(true);
        given(companySubscriptionRepository.findByCompanyId(COMPANY_ID)).willReturn(Optional.of(subscription));
        return company;
    }

    private static final CompanySubscription FREE = CompanySubscription.freeForUser(LocalDate.now());        // 카테고리 상한 1, 동영상 불가, 커스텀 색 불가
    private static final CompanySubscription STANDARD = CompanySubscription.active(CompanyGrade.STANDARD, LocalDate.now(), null); // 상한 5, 동영상/커스텀 색 허용
    private static final CompanySubscription ADMIN_EXEMPT = CompanySubscription.adminExempt(LocalDate.now()); // 등급 한도 면제

    // 기본정보 PATCH 요청 — videoUrl / brandColor 만 관심.
    private CompanyUpdateRequest basicInfo(String videoUrl, String brandColor) {
        return new CompanyUpdateRequest(null, null, null, null, null, null, null, null, null, null, null, null,
                videoUrl, null, null, null, brandColor);
    }

    // 신원 필드(대표자명/개업일자)만 건드리는 수정 요청.
    private CompanyUpdateRequest identityInfo(String ceoName, LocalDate establishmentDate) {
        return new CompanyUpdateRequest(null, ceoName, establishmentDate, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }

    @Nested
    @DisplayName("국세청 인증 회사의 신원 필드 보호")
    class VerifiedIdentityGuard {

        @Test
        @DisplayName("인증받은 회사의 대표자명 변경은 막는다 — 통과 후 값만 바꿔 배지를 유지하는 우회 차단")
        void verifiedCompany_cannotChangeCeoName() {
            Company company = givenOwnedCompany(FREE);
            company.markBusinessVerified(LocalDateTime.now());

            assertThatThrownBy(() -> service.updateBasicInfoByUser(COMPANY_ID, OWNER_ID, identityInfo("다른대표", null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CompanyErrorCode.VERIFIED_FIELD_NOT_EDITABLE);

            verify(searchDocumentWriter, never()).write(any());
        }

        @Test
        @DisplayName("인증받은 회사의 개업일자 변경도 막는다 — 업력 뻥튀기 차단")
        void verifiedCompany_cannotChangeEstablishmentDate() {
            Company company = givenOwnedCompany(FREE);
            company.markBusinessVerified(LocalDateTime.now());

            assertThatThrownBy(() -> service.updateBasicInfoByUser(COMPANY_ID, OWNER_ID,
                    identityInfo(null, LocalDate.of(1990, 1, 1))))
                    .extracting("errorCode")
                    .isEqualTo(CompanyErrorCode.VERIFIED_FIELD_NOT_EDITABLE);
        }

        @Test
        @DisplayName("같은 값을 다시 보내는 건 변경이 아니므로 통과시킨다 (전체 폼 재전송 대응)")
        void verifiedCompany_sameValueIsNotAChange() {
            Company company = givenOwnedCompany(FREE);
            company.markBusinessVerified(LocalDateTime.now());

            assertThatCode(() -> service.updateBasicInfoByUser(COMPANY_ID, OWNER_ID,
                    identityInfo(company.getCeoName(), company.getEstablishmentDate())))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("미인증 회사(관리자 등록 등)는 대표자명을 자유롭게 수정할 수 있다")
        void unverifiedCompany_canChangeCeoName() {
            Company company = givenOwnedCompany(FREE);

            service.updateBasicInfoByUser(COMPANY_ID, OWNER_ID, identityInfo("새대표", null));

            assertThat(company.getCeoName()).isEqualTo("새대표");
        }
    }

    @Nested
    @DisplayName("사업자 인증 회수 (관리자)")
    class RevokeBusinessVerification {

        @Test
        @DisplayName("회사 플래그를 내리고 인증 레코드를 사유와 함께 REVOKED 로 남긴다")
        void revoke_clearsFlagAndRecordsReason() {
            Company company = CompanyFixture.userCompany();
            company.markBusinessVerified(LocalDateTime.now());
            given(companyRepository.findById(COMPANY_ID)).willReturn(Optional.of(company));
            CompanyVerification verification = CompanyVerificationFixture.consumed(9L, OWNER_ID, COMPANY_ID);
            given(verificationRepository.findByCompanyId(COMPANY_ID)).willReturn(Optional.of(verification));

            service.revokeBusinessVerificationByAdmin(COMPANY_ID, "사칭 신고");

            assertThat(company.isBusinessVerified()).isFalse();
            assertThat(company.getBusinessVerifiedAt()).isNull();
            // 미인증으로 되돌리지 않고 REVOKED 로 남긴다 — 같은 번호의 즉시 재인증을 막는 근거가 된다.
            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.REVOKED);
            assertThat(verification.getRevokedReason()).isEqualTo("사칭 신고");
        }

        @Test
        @DisplayName("사업자번호는 지우지 않는다 — 활성 유니크로 재등록을 계속 막고 분쟁 기록도 남겨야 한다")
        void revoke_keepsBusinessNumber() {
            Company company = CompanyFixture.userCompany();
            company.markBusinessVerified(LocalDateTime.now());
            String before = company.getBusinessNumber();
            given(companyRepository.findById(COMPANY_ID)).willReturn(Optional.of(company));
            given(verificationRepository.findByCompanyId(COMPANY_ID)).willReturn(Optional.empty());

            service.revokeBusinessVerificationByAdmin(COMPANY_ID, "사유");

            assertThat(company.getBusinessNumber()).isEqualTo(before);
        }
    }

    @Nested
    @DisplayName("컬렉션 교체(카테고리) - 등급 한도 검증")
    class CategoryLimit {

        @Test
        @DisplayName("FREE 회사가 카테고리 상한(1)을 넘겨 교체하면 CATEGORY_LIMIT_EXCEEDED 로 막고 검색 동기화도 안 한다")
        void free_overLimit_blocked() {
            givenOwnedCompany(FREE);

            assertThatThrownBy(() -> service.replaceCategoriesByUser(COMPANY_ID, OWNER_ID, List.of(1L, 2L)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CompanyErrorCode.CATEGORY_LIMIT_EXCEEDED);

            verify(searchDocumentWriter, never()).write(any());
        }

        @Test
        @DisplayName("FREE 회사가 상한(1) 이내로 교체하면 통과하고 교체·검색 동기화가 일어난다")
        void free_withinLimit_passes() {
            Company company = givenOwnedCompany(FREE);

            assertThatCode(() -> service.replaceCategoriesByUser(COMPANY_ID, OWNER_ID, List.of(1L)))
                    .doesNotThrowAnyException();

            verify(linkWriter).replaceCategories(company, List.of(1L));
            verify(searchDocumentWriter).write(COMPANY_ID);
        }

        @Test
        @DisplayName("ADMIN_EXEMPT 회사는 카테고리 상한을 넘겨도 면제되어 통과한다")
        void adminExemptCompany_overLimit_passes() {
            givenOwnedCompany(ADMIN_EXEMPT);

            assertThatCode(() -> service.replaceCategoriesByUser(COMPANY_ID, OWNER_ID, List.of(1L, 2L, 3L)))
                    .doesNotThrowAnyException();

            verify(searchDocumentWriter).write(COMPANY_ID);
        }

        @Test
        @DisplayName("관리자 경로도 '회사의 구독'을 기준으로 판정한다 — FREE 회사면 관리자 수정도 상한에 걸린다")
        void adminPath_boundByCompanySubscription() {
            given(companyRepository.findById(COMPANY_ID)).willReturn(Optional.of(CompanyFixture.userCompany()));
            given(companySubscriptionRepository.findByCompanyId(COMPANY_ID)).willReturn(Optional.of(FREE));

            assertThatThrownBy(() -> service.replaceCategoriesByAdmin(COMPANY_ID, List.of(1L, 2L)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CompanyErrorCode.CATEGORY_LIMIT_EXCEEDED);

            verify(searchDocumentWriter, never()).write(any());
        }
    }

    @Nested
    @DisplayName("기본정보 수정 - 게이팅/변형")
    class BasicInfo {

        @Test
        @DisplayName("FREE 회사가 videoUrl 을 넣어 수정하면 VIDEO_NOT_ALLOWED 로 막는다")
        void free_video_blocked() {
            givenOwnedCompany(FREE);

            assertThatThrownBy(() -> service.updateBasicInfoByUser(COMPANY_ID, OWNER_ID, basicInfo("https://youtu.be/x", null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(CompanyErrorCode.VIDEO_NOT_ALLOWED);

            verify(searchDocumentWriter, never()).write(any());
        }

        @Test
        @DisplayName("FREE 회사가 커스텀 brandColor 를 넣으면 거부가 아니라 기본값으로 고정(변형)하고 통과시킨다")
        void free_brandColor_overriddenToDefault() {
            Company company = givenOwnedCompany(FREE);

            service.updateBasicInfoByUser(COMPANY_ID, OWNER_ID, basicInfo(null, "#FF0000"));

            assertThat(company.getBrandColor()).isEqualTo(DEFAULT_BRAND_COLOR);
            verify(searchDocumentWriter).write(COMPANY_ID);
        }

        @Test
        @DisplayName("커스텀 허용 등급(STANDARD)은 요청한 brandColor 를 유지하고 videoUrl 도 통과시킨다")
        void standard_keepsColorAndAllowsVideo() {
            Company company = givenOwnedCompany(STANDARD);

            service.updateBasicInfoByUser(COMPANY_ID, OWNER_ID, basicInfo("https://youtu.be/x", "#FF0000"));

            assertThat(company.getBrandColor()).isEqualTo("#FF0000");
            verify(searchDocumentWriter).write(COMPANY_ID);
        }
    }

    @Test
    @DisplayName("소유자가 아니면 정책 이전에 COMPANY_ACCESS_DENIED 로 막는다")
    void notOwner_accessDenied() {
        given(companyRepository.findById(COMPANY_ID)).willReturn(Optional.of(CompanyFixture.userCompany()));
        given(companyMemberRepository.existsByCompanyIdAndUserId(COMPANY_ID, OWNER_ID)).willReturn(false);

        assertThatThrownBy(() -> service.replaceCategoriesByUser(COMPANY_ID, OWNER_ID, List.of(1L, 2L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CompanyErrorCode.COMPANY_ACCESS_DENIED);

        verify(searchDocumentWriter, never()).write(any());
    }
}
