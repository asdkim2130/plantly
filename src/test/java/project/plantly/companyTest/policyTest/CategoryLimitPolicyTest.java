package project.plantly.companyTest.policyTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import project.plantly.companyTest.support.CompanyCreateRequestBuilder;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.CompanyRegistrationContext;
import project.plantly.domain.company.policy.rule.CategoryLimitPolicy;
import project.plantly.domain.user.enums.UserGrade;
import project.plantly.domain.user.policy.GradePolicyRegistry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// CategoryLimitPolicy: 중복 제거 후 카테고리 개수가 등급 상한을 넘으면 거부.
@DisplayName("CategoryLimitPolicy: 카테고리 개수 상한")
class CategoryLimitPolicyTest {

    private final CategoryLimitPolicy policy = new CategoryLimitPolicy(new GradePolicyRegistry());

    private final CompanyRegistrationContext free = CompanyRegistrationContext.ofUser();           // FREE, 상한 1
    private final CompanyRegistrationContext premium = new CompanyRegistrationContext(UserGrade.PREMIUM); // 상한 10
    private final CompanyRegistrationContext admin = CompanyRegistrationContext.ofAdmin();          // 면제

    @Test
    @DisplayName("FREE 상한(1) 이내면 통과")
    void free_withinLimit_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest()
                .categoryIds(List.of(1L)).build();

        assertThatCode(() -> policy.apply(null, request, free)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("FREE 상한(1) 초과면 CATEGORY_LIMIT_EXCEEDED")
    void free_overLimit_throws() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest()
                .categoryIds(List.of(1L, 2L)).build();

        assertThatThrownBy(() -> policy.apply(null, request, free))
                .extracting("errorCode")
                .isEqualTo(CompanyErrorCode.CATEGORY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("중복 ID 는 distinct 후 1개로 계산되어 FREE 상한을 넘지 않는다")
    void free_duplicatesCountedOnce_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest()
                .categoryIds(List.of(1L, 1L, 1L)).build();

        assertThatCode(() -> policy.apply(null, request, free)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("categoryIds 가 null 이면 검증 없이 통과")
    void nullCategoryIds_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().build();

        assertThatCode(() -> policy.apply(null, request, free)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("상위 등급(PREMIUM, 상한 10)은 더 많은 카테고리를 허용")
    void premium_higherLimit_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest()
                .categoryIds(List.of(1L, 2L, 3L, 4L, 5L)).build();

        assertThatCode(() -> policy.apply(null, request, premium)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("관리자 등록은 상한을 초과해도 면제되어 통과")
    void admin_exempt_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest()
                .categoryIds(List.of(1L, 2L, 3L)).build();

        assertThatCode(() -> policy.apply(null, request, admin)).doesNotThrowAnyException();
    }
}
