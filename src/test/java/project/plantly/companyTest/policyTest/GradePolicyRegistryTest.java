package project.plantly.companyTest.policyTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.policy.GradePolicy;
import project.plantly.domain.company.policy.GradePolicyRegistry;

import static org.assertj.core.api.Assertions.assertThat;

// GradePolicyRegistry: "등급 → 정책" 매핑의 단일 출처. 매핑 값과 누락 처리를 검증한다.
// 등급별 outcome 단언은 이 테이블 값에 의존하므로, 값이 바뀌면 여기서 먼저 깨지도록 못 박는다.
@DisplayName("GradePolicyRegistry: 등급별 정책 매핑")
class GradePolicyRegistryTest {

    private final GradePolicyRegistry registry = new GradePolicyRegistry();

    @Test
    @DisplayName("FREE 등급의 정책 값")
    void free() {
        GradePolicy policy = registry.of(CompanyGrade.FREE);

        assertThat(policy.maxCompanyCategories()).isEqualTo(1);
        assertThat(policy.videoAllowed()).isFalse();
        assertThat(policy.maxReferenceImages()).isEqualTo(0);
        assertThat(policy.maxDetailImages()).isEqualTo(3);
        assertThat(policy.customBrandColorAllowed()).isFalse();
        assertThat(policy.spotlightOnCreate()).isFalse();
    }

    @Test
    @DisplayName("PREMIUM 등급의 정책 값")
    void premium() {
        GradePolicy policy = registry.of(CompanyGrade.PREMIUM);

        assertThat(policy.maxCompanyCategories()).isEqualTo(10);
        assertThat(policy.videoAllowed()).isTrue();
        assertThat(policy.maxReferenceImages()).isEqualTo(0);
        assertThat(policy.maxDetailImages()).isEqualTo(20);
        assertThat(policy.customBrandColorAllowed()).isTrue();
        assertThat(policy.spotlightOnCreate()).isTrue();
    }

    @Test
    @DisplayName("ENTERPRISE 등급의 정책 값")
    void enterprise() {
        GradePolicy policy = registry.of(CompanyGrade.ENTERPRISE);

        assertThat(policy.maxCompanyCategories()).isEqualTo(10);
        assertThat(policy.videoAllowed()).isTrue();
        assertThat(policy.maxReferenceImages()).isEqualTo(10);
        assertThat(policy.maxDetailImages()).isEqualTo(30);
        assertThat(policy.customBrandColorAllowed()).isTrue();
        assertThat(policy.spotlightOnCreate()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = CompanyGrade.class, names = {"FREE", "BASIC", "STANDARD", "PREMIUM", "ENTERPRISE"})
    @DisplayName("정책이 정의된 모든 등급은 조회 가능하다")
    void definedGrades_returnPolicy(CompanyGrade grade) {
        assertThat(registry.of(grade)).isNotNull();
    }
}
