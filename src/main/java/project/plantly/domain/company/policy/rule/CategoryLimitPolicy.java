package project.plantly.domain.company.policy.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.CompanyRegistrationContext;
import project.plantly.domain.company.policy.CompanyRegistrationPolicy;
import project.plantly.domain.user.policy.GradePolicyRegistry;
import project.plantly.global.exception.BusinessException;

import java.util.List;

// 정책: 실제 저장될 카테고리(중복 제거 후) 개수가 등급별 상한을 넘으면 등록을 거부한다.
// - 유저 자가등록: 본인 등급의 상한을 따른다.
// - 관리자 선등록: 소유자(=등급) 가 없으므로 제품 절대 상한(최상위 등급) 으로 캡한다.
// 등급별 상한 값 자체는 GradePolicyRegistry 가 소유하며, 이 정책은 "어떤 등급 기준으로 볼지" 만 결정한다.
@Component
@RequiredArgsConstructor
public class CategoryLimitPolicy implements CompanyRegistrationPolicy {

    private final GradePolicyRegistry gradePolicyRegistry;

    @Override
    public void apply(Company company, CompanyCreateRequest request, CompanyRegistrationContext context) {
        List<Long> categoryIds = request.categoryIds();
        if (categoryIds == null) {
            return;
        }

        int maxCategories = resolveMaxCategories(context);

        long distinctCount = categoryIds.stream().distinct().count();
        if (distinctCount > maxCategories) {
            throw new BusinessException(CompanyErrorCode.CATEGORY_LIMIT_EXCEEDED);
        }
    }

    private int resolveMaxCategories(CompanyRegistrationContext context) {
        return gradePolicyRegistry.of(context.gradeForCap()).maxCompanyCategories();
    }
}