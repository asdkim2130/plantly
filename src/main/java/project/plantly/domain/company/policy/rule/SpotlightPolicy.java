package project.plantly.domain.company.policy.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.policy.CompanyRegistrationPolicy;
import project.plantly.domain.company.policy.GradePolicyRegistry;

// 정책(변형): 등록 시점 spotlight 자동 활성화 등급이면 company 의 spotlight 를 켠다.
// 관리자 등록(ADMIN_EXEMPT)은 면제되므로 제외한다. (spotlight 는 추후 admin 큐레이션으로 별도 관리)
@Component
@RequiredArgsConstructor
public class SpotlightPolicy implements CompanyRegistrationPolicy {

    private final GradePolicyRegistry gradePolicyRegistry;

    @Override
    public void apply(Company company, CompanyCreateRequest request, CompanySubscription subscription) {
        if (subscription.isExempt()) {
            return;
        }

        if (gradePolicyRegistry.of(subscription.effectiveGrade()).spotlightOnCreate()) {
            company.activateSpotlight();
        }
    }
}
