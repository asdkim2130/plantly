package project.plantly.domain.company.policy.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.enums.ImageType;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.CompanyMutationPolicy;
import project.plantly.domain.company.policy.CompanyPolicyView;
import project.plantly.domain.company.policy.GradePolicyRegistry;
import project.plantly.global.exception.BusinessException;

// 정책: 회사 직속 갤러리의 상세 이미지(ImageType.DETAIL) 장수가 등급별 상한을 넘으면 거부한다. (등록·수정 공통)
// - 유저: 회사 구독 등급(FREE 등)의 상한을 따른다.
// - 관리자 등록(ADMIN_EXEMPT): 등급 한도 면제 — 스킵한다. 다만 '무제한' 은 아니다:
//   요청 DTO 가 최고 등급값을 절대 천장으로 걸어두므로(CompanyConstraints 의 CEILING) 면제 회사도 그 수는 넘지 못한다.
// galleryImages 리스트 중 imageType == DETAIL 인 것만 센다. (PROJECT 이미지는 references 경로로 들어오므로 제외)
// 등급별 상한 값 자체는 GradePolicyRegistry 가 소유한다.
@Component
@RequiredArgsConstructor
public class DetailImageLimitPolicy implements CompanyMutationPolicy {

    private final GradePolicyRegistry gradePolicyRegistry;

    @Override
    public void apply(CompanyPolicyView view) {
        if (view.subscription().isExempt()) {
            return;
        }

        if (view.galleryImages() == null) {
            return;
        }

        long detailCount = view.galleryImages().stream()
                .filter(image -> image.imageType() == ImageType.DETAIL)
                .count();

        int maxDetailImages = gradePolicyRegistry.of(view.subscription().effectiveGrade()).maxDetailImages();
        if (detailCount > maxDetailImages) {
            throw new BusinessException(CompanyErrorCode.DETAIL_IMAGE_LIMIT_EXCEEDED);
        }
    }
}
