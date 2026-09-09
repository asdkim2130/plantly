package project.plantly.companyTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import project.plantly.domain.company.dto.CompanyConstraints;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.policy.GradePolicy;
import project.plantly.domain.company.policy.GradePolicyRegistry;

import static org.assertj.core.api.Assertions.assertThat;

// 등급 한도(GradePolicyRegistry)와 DTO 천장(CompanyConstraints)에 같은 성격의 숫자가 두 곳에 산다.
// @Size 는 컴파일 타임 상수만 받아 레지스트리 값을 참조할 수 없어서 생긴 중복이다.
//
// 위험한 방향은 하나뿐이다 - 레지스트리 값이 천장보다 커지는 것. 그러면 등급 정책을 올려도 DTO 가
// 조용히 먼저 막아 정책 변경이 무력화되고, 화면에는 "PREMIUM 은 15개까지" 라고 안내하면서 15개를 보내면
// 400 이 나는 상태가 된다. 반대 방향(천장이 더 큼)은 정상이다 - 천장은 등급 한도가 아니라
// 명백한 남용을 끊는 절대 상한이고, 등급별 판단은 서비스 계층 정책이 한다.
@DisplayName("등급 한도는 DTO 천장을 넘지 않는다")
class CompanyConstraintsCeilingTest {

    private final GradePolicyRegistry registry = new GradePolicyRegistry();

    @ParameterizedTest(name = "{0}")
    @EnumSource(CompanyGrade.class)
    @DisplayName("모든 등급의 한도가 대응하는 천장 이하다")
    void gradeLimitsStayUnderCeiling(CompanyGrade grade) {
        GradePolicy policy = registry.of(grade);

        assertThat(policy.maxCompanyCategories())
                .as("%s 의 카테고리 한도가 CompanyConstraints.CATEGORIES_CEILING 을 넘는다", grade)
                .isLessThanOrEqualTo(CompanyConstraints.CATEGORIES_CEILING);

        assertThat(policy.maxDetailImages())
                .as("%s 의 상세 이미지 한도가 CompanyConstraints.DETAIL_IMAGES_CEILING 을 넘는다", grade)
                .isLessThanOrEqualTo(CompanyConstraints.DETAIL_IMAGES_CEILING);

        assertThat(policy.maxReferenceImages())
                .as("%s 의 레퍼런스 이미지 한도가 CompanyConstraints.REFERENCE_IMAGES_CEILING 을 넘는다", grade)
                .isLessThanOrEqualTo(CompanyConstraints.REFERENCE_IMAGES_CEILING);
    }
}
