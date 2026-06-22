package project.plantly.domain.company.policy.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.policy.CompanyRegistrationContext;
import project.plantly.domain.company.policy.CompanyRegistrationPolicy;
import project.plantly.domain.user.policy.GradePolicyRegistry;

// 정책(변형): 등록 시점 spotlight 자동 활성화 등급이면 company 의 spotlight 를 켠다.
// 관리자 등록은 등급이 없으므로 제외한다. (spotlight 는 추후 admin 큐레이션으로 별도 관리)
@Component
@RequiredArgsConstructor
public class SpotlightPolicy implements CompanyRegistrationPolicy {

    private final GradePolicyRegistry gradePolicyRegistry;

    @Override
    public void apply(Company company, CompanyCreateRequest request, CompanyRegistrationContext context) {
        if (context.isAdminRegistration()) {
            return;
        }

        if (gradePolicyRegistry.of(context.grade()).spotlightOnCreate()) {
            company.activateSpotlight();
        }
    }
}