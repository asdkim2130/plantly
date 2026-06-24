package project.plantly.companyTest.policyTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import project.plantly.companyTest.support.CompanyCreateRequestBuilder;
import project.plantly.companyTest.support.CompanyFixture;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.policy.CompanyRegistrationContext;
import project.plantly.domain.company.policy.rule.BrandColorPolicy;
import project.plantly.domain.user.enums.UserGrade;
import project.plantly.domain.user.policy.GradePolicyRegistry;

import static org.assertj.core.api.Assertions.assertThat;

// BrandColorPolicy(변형): 커스텀 불가 등급은 brandColor 를 기본값(#808080)으로 덮어쓴다. 거부가 아니라 고정.
@DisplayName("BrandColorPolicy: 브랜드 컬러 고정")
class BrandColorPolicyTest {

    private static final String DEFAULT_BRAND_COLOR = "#808080";

    private final BrandColorPolicy policy = new BrandColorPolicy(new GradePolicyRegistry());

    private final CompanyRegistrationContext free = CompanyRegistrationContext.ofUser();              // 커스텀 불가
    private final CompanyRegistrationContext standard = new CompanyRegistrationContext(UserGrade.STANDARD); // 커스텀 허용
    private final CompanyRegistrationContext admin = CompanyRegistrationContext.ofAdmin();

    @Test
    @DisplayName("FREE 는 요청한 색을 무시하고 기본값으로 고정한다")
    void free_overridesToDefault() {
        Company company = CompanyFixture.userCompanyWithBrandColor("#FF0000");
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().brandColor("#FF0000").build();

        policy.apply(company, request, free);

        assertThat(company.getBrandColor()).isEqualTo(DEFAULT_BRAND_COLOR);
    }

    @Test
    @DisplayName("커스텀 허용 등급(STANDARD)은 요청한 색을 유지한다")
    void standard_keepsRequestedColor() {
        Company company = CompanyFixture.userCompanyWithBrandColor("#FF0000");
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().brandColor("#FF0000").build();

        policy.apply(company, request, standard);

        assertThat(company.getBrandColor()).isEqualTo("#FF0000");
    }

    @Test
    @DisplayName("관리자 등록은 면제되어 요청한 색을 유지한다")
    void admin_keepsRequestedColor() {
        Company company = CompanyFixture.userCompanyWithBrandColor("#FF0000");
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().brandColor("#FF0000").build();

        policy.apply(company, request, admin);

        assertThat(company.getBrandColor()).isEqualTo("#FF0000");
    }
}
