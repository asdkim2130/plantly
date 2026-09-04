package project.plantly.domain.company.policy.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.CompanyMutationPolicy;
import project.plantly.domain.company.policy.CompanyPolicyView;
import project.plantly.domain.company.policy.GradePolicyRegistry;
import project.plantly.global.exception.BusinessException;

import java.util.List;

// 정책: 실제 저장될 카테고리(중복 제거 후) 개수가 등급별 상한을 넘으면 거부한다. (등록·수정 공통)
// - 유저: 회사 구독 등급(FREE 등)의 상한을 따른다.
// - 관리자 등록(ADMIN_EXEMPT): 등급 한도 면제 — 스킵한다. 다만 '무제한' 은 아니다:
//   요청 DTO 가 최고 등급값을 절대 천장으로 걸어두므로(CompanyConstraints 의 CEILING) 면제 회사도 그 수는 넘지 못한다.
// 등급별 상한 값 자체는 GradePolicyRegistry 가 소유한다.
@Component
@RequiredArgsConstructor
public class CategoryLimitPolicy implements CompanyMutationPolicy {

    private final GradePolicyRegistry gradePolicyRegistry;

    @Override
    public void apply(CompanyPolicyView view) {
        if (view.subscription().isExempt()) {
            return;
        }

        List<Long> categoryIds = view.categoryIds();
        if (categoryIds == null) {
            return;
        }

        int maxCategories = gradePolicyRegistry.of(view.subscription().effectiveGrade()).maxCompanyCategories();

        long distinctCount = categoryIds.stream().distinct().count();
        if (distinctCount > maxCategories) {
            throw new BusinessException(CompanyErrorCode.CATEGORY_LIMIT_EXCEEDED);
        }
    }
}
