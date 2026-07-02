package project.plantly.domain.company.policy.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.CompanyRegistrationPolicy;
import project.plantly.domain.company.policy.GradePolicyRegistry;
import project.plantly.global.exception.BusinessException;

import java.util.List;

// 정책: 실제 저장될 카테고리(중복 제거 후) 개수가 등급별 상한을 넘으면 등록을 거부한다.
// - 유저 자가등록: 회사 구독 등급(FREE)의 상한을 따른다.
// - 관리자 등록(ADMIN_EXEMPT): 등급 한도 면제 — 스킵한다.
// 등급별 상한 값 자체는 GradePolicyRegistry 가 소유한다.
@Component
@RequiredArgsConstructor
public class CategoryLimitPolicy implements CompanyRegistrationPolicy {

    private final GradePolicyRegistry gradePolicyRegistry;

    @Override
    public void apply(Company company, CompanyCreateRequest request, CompanySubscription subscription) {
        if (subscription.isExempt()) {
            return;
        }

        List<Long> categoryIds = request.categoryIds();
        if (categoryIds == null) {
            return;
        }

        int maxCategories = gradePolicyRegistry.of(subscription.effectiveGrade()).maxCompanyCategories();

        long distinctCount = categoryIds.stream().distinct().count();
        if (distinctCount > maxCategories) {
            throw new BusinessException(CompanyErrorCode.CATEGORY_LIMIT_EXCEEDED);
        }
    }
}
