package project.plantly.domain.company.policy.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.enums.ImageType;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.CompanyRegistrationPolicy;
import project.plantly.domain.company.policy.GradePolicyRegistry;
import project.plantly.global.exception.BusinessException;

// 정책: 회사 직속 갤러리의 상세 이미지(ImageType.DETAIL) 장수가 등급별 상한을 넘으면 등록을 거부한다.
// - 유저 자가등록: 회사 구독 등급(FREE)의 상한을 따른다.
// - 관리자 등록(ADMIN_EXEMPT): 등급 한도 면제 — 스킵한다.
// images 리스트 중 imageType == DETAIL 인 것만 센다. (PROJECT 이미지는 references 경로로 들어오므로 제외)
// 등급별 상한 값 자체는 GradePolicyRegistry 가 소유한다.
@Component
@RequiredArgsConstructor
public class DetailImageLimitPolicy implements CompanyRegistrationPolicy {

    private final GradePolicyRegistry gradePolicyRegistry;

    @Override
    public void apply(Company company, CompanyCreateRequest request, CompanySubscription subscription) {
        if (subscription.isExempt()) {
            return;
        }

        if (request.images() == null) {
            return;
        }

        long detailCount = request.images().stream()
                .filter(image -> image.imageType() == ImageType.DETAIL)
                .count();

        int maxDetailImages = gradePolicyRegistry.of(subscription.effectiveGrade()).maxDetailImages();
        if (detailCount > maxDetailImages) {
            throw new BusinessException(CompanyErrorCode.DETAIL_IMAGE_LIMIT_EXCEEDED);
        }
    }
}
