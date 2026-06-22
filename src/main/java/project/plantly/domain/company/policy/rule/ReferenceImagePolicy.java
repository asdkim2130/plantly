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

// 정책(검증): 레퍼런스 이미지 업로드 가능 여부와 레퍼런스 1건당 최대 장수를 등급별로 강제한다.
// - maxReferenceImages == 0 : 업로드 비활성. 이미지를 보내면 거부.
// - 그 외 : 레퍼런스 1건이라도 상한을 넘으면 거부.
// 관리자 등록은 등급이 없으므로 이 게이팅에서 제외한다.
@Component
@RequiredArgsConstructor
public class ReferenceImagePolicy implements CompanyRegistrationPolicy {

    private final GradePolicyRegistry gradePolicyRegistry;

    @Override
    public void apply(Company company, CompanyCreateRequest request, CompanyRegistrationContext context) {
        if (context.isAdminRegistration() || request.references() == null) {
            return;
        }

        int maxReferenceImages = gradePolicyRegistry.of(context.grade()).maxReferenceImages();

        for (CompanyCreateRequest.ReferenceRequest reference : request.references()) {
            List<String> imageUrls = reference.imageUrls();
            if (imageUrls == null || imageUrls.isEmpty()) {
                continue;
            }
            if (maxReferenceImages == 0) {
                throw new BusinessException(CompanyErrorCode.REFERENCE_IMAGE_NOT_ALLOWED);
            }
            if (imageUrls.size() > maxReferenceImages) {
                throw new BusinessException(CompanyErrorCode.REFERENCE_IMAGE_LIMIT_EXCEEDED);
            }
        }
    }
}