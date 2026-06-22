package project.plantly.domain.company.policy.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.CompanyRegistrationContext;
import project.plantly.domain.company.policy.CompanyRegistrationPolicy;
import project.plantly.domain.user.policy.GradePolicy;
import project.plantly.domain.user.policy.GradePolicyRegistry;
import project.plantly.global.exception.BusinessException;

// 정책(검증): 동영상 사용이 허용되지 않는 등급이 videoUrl 을 보내면 거부한다.
// 관리자 등록은 등급이 없으므로 이 게이팅에서 제외한다.
@Component
@RequiredArgsConstructor
public class VideoUrlPolicy implements CompanyRegistrationPolicy {

    private final GradePolicyRegistry gradePolicyRegistry;

    @Override
    public void apply(Company company, CompanyCreateRequest request, CompanyRegistrationContext context) {
        if (context.isAdminRegistration()) {
            return;
        }

        String videoUrl = request.videoUrl();
        if (videoUrl == null || videoUrl.isBlank()) {
            return;
        }

        GradePolicy gradePolicy = gradePolicyRegistry.of(context.grade());
        if (!gradePolicy.videoAllowed()) {
            throw new BusinessException(CompanyErrorCode.VIDEO_NOT_ALLOWED);
        }
    }
}