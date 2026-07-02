package project.plantly.domain.company.policy.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.CompanyRegistrationPolicy;
import project.plantly.domain.company.policy.GradePolicy;
import project.plantly.domain.company.policy.GradePolicyRegistry;
import project.plantly.global.exception.BusinessException;

// 정책(검증): 동영상 사용이 허용되지 않는 등급이 videoUrl 을 보내면 거부한다.
// 관리자 등록(ADMIN_EXEMPT)은 등급 한도에서 면제되므로 이 게이팅에서 제외한다.
@Component
@RequiredArgsConstructor
public class VideoUrlPolicy implements CompanyRegistrationPolicy {

    private final GradePolicyRegistry gradePolicyRegistry;

    @Override
    public void apply(Company company, CompanyCreateRequest request, CompanySubscription subscription) {
        if (subscription.isExempt()) {
            return;
        }

        String videoUrl = request.videoUrl();
        if (videoUrl == null || videoUrl.isBlank()) {
            return;
        }

        GradePolicy gradePolicy = gradePolicyRegistry.of(subscription.effectiveGrade());
        if (!gradePolicy.videoAllowed()) {
            throw new BusinessException(CompanyErrorCode.VIDEO_NOT_ALLOWED);
        }
    }
}
