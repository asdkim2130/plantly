package project.plantly.domain.company.policy.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.policy.CompanyRegistrationPolicy;
import project.plantly.domain.company.policy.GradePolicyRegistry;

// 정책(변형): 커스텀 색상이 허용되지 않는 등급은 brandColor 를 기본값(Grey)으로 고정한다.
// 요청 값을 거부하지 않고 덮어쓴다("고정"). 관리자 등록(ADMIN_EXEMPT)은 면제되므로 제외한다.
@Component
@RequiredArgsConstructor
public class BrandColorPolicy implements CompanyRegistrationPolicy {

    // 커스텀 불가 등급의 기본 브랜드 컬러. 표현 형식이 바뀌면 이 값만 조정한다.
    private static final String DEFAULT_BRAND_COLOR = "#808080";

    private final GradePolicyRegistry gradePolicyRegistry;

    @Override
    public void apply(Company company, CompanyCreateRequest request, CompanySubscription subscription) {
        if (subscription.isExempt()) {
            return;
        }

        if (!gradePolicyRegistry.of(subscription.effectiveGrade()).customBrandColorAllowed()) {
            company.changeBrandColor(DEFAULT_BRAND_COLOR);
        }
    }
}
