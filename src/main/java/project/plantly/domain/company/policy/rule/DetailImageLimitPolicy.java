package project.plantly.domain.company.policy.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.enums.ImageType;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.CompanyRegistrationContext;
import project.plantly.domain.company.policy.CompanyRegistrationPolicy;
import project.plantly.domain.user.policy.GradePolicyRegistry;
import project.plantly.global.exception.BusinessException;

// 정책: 회사 직속 갤러리의 상세 이미지(ImageType.DETAIL) 장수가 등급별 상한을 넘으면 등록을 거부한다.
// - 유저 자가등록: 본인 회사 등급(FREE)의 상한을 따른다.
// - 관리자 등록(ADMIN_REGISTER): 등급 한도 면제 — 스킵한다.
// images 리스트 중 imageType == DETAIL 인 것만 센다. (PROJECT 이미지는 references 경로로 들어오므로 제외)
// 등급별 상한 값 자체는 GradePolicyRegistry 가 소유한다.
@Component
@RequiredArgsConstructor
public class DetailImageLimitPolicy implements CompanyRegistrationPolicy {

    private final GradePolicyRegistry gradePolicyRegistry;

    @Override
    public void apply(Company company, CompanyCreateRequest request, CompanyRegistrationContext context) {
        if (context.isAdminRegistration()) {
            return;
        }

        if (request.images() == null) {
            return;
        }

        long detailCount = request.images().stream()
                .filter(image -> image.imageType() == ImageType.DETAIL)
                .count();

        int maxDetailImages = gradePolicyRegistry.of(context.grade()).maxDetailImages();
        if (detailCount > maxDetailImages) {
            throw new BusinessException(CompanyErrorCode.DETAIL_IMAGE_LIMIT_EXCEEDED);
        }
    }
}