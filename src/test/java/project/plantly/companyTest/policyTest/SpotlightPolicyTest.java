package project.plantly.companyTest.policyTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import project.plantly.companyTest.support.CompanyCreateRequestBuilder;
import project.plantly.companyTest.support.CompanyFixture;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.policy.GradePolicyRegistry;
import project.plantly.domain.company.policy.rule.SpotlightPolicy;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// SpotlightPolicy(변형): 등록 시점 spotlight 자동 활성화 등급이면 켠다. 그 외 등급/관리자는 끈 상태 유지.
@DisplayName("SpotlightPolicy: 등록 시 spotlight 자동 활성화")
class SpotlightPolicyTest {

    private final SpotlightPolicy policy = new SpotlightPolicy(new GradePolicyRegistry());

    private final CompanySubscription free = CompanySubscription.freeForUser(LocalDate.now());              // 자동 활성화 아님
    private final CompanySubscription premium = CompanySubscription.active(CompanyGrade.PREMIUM, LocalDate.now(), null); // 자동 활성화
    private final CompanySubscription admin = CompanySubscription.adminExempt(LocalDate.now());

    private final CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().build();

    @Test
    @DisplayName("자동 활성화 등급(PREMIUM)은 spotlight 를 켠다")
    void premium_activatesSpotlight() {
        Company company = CompanyFixture.userCompany();

        policy.apply(company, request, premium);

        assertThat(company.isSpotlight()).isTrue();
    }

    @Test
    @DisplayName("FREE 는 spotlight 를 켜지 않는다")
    void free_keepsSpotlightOff() {
        Company company = CompanyFixture.userCompany();

        policy.apply(company, request, free);

        assertThat(company.isSpotlight()).isFalse();
    }

    @Test
    @DisplayName("관리자 등록은 면제되어 spotlight 를 켜지 않는다")
    void admin_keepsSpotlightOff() {
        Company company = CompanyFixture.userCompany();

        policy.apply(company, request, admin);

        assertThat(company.isSpotlight()).isFalse();
    }
}
