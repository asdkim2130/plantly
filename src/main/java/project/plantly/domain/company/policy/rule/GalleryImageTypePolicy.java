package project.plantly.domain.company.policy.rule;

import org.springframework.stereotype.Component;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.enums.ImageType;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.CompanyRegistrationPolicy;
import project.plantly.global.exception.BusinessException;

// 정책(구조 검증): 회사 직속 갤러리(images)에는 상세 이미지(ImageType.DETAIL)만 허용한다.
// PROJECT 이미지는 references 경로로만 들어와야 하므로, images 에 DETAIL 외(또는 null) 타입이 섞이면 거부한다.
// 이를 통해 DetailImageLimitPolicy 의 DETAIL-only 카운팅이 타입 우회로 무력화되는 것을 막는다.
// 등급과 무관한 구조 검증이므로 면제(관리자 등록) 여부와 상관없이 전원 적용한다.
@Component
public class GalleryImageTypePolicy implements CompanyRegistrationPolicy {

    @Override
    public void apply(Company company, CompanyCreateRequest request, CompanySubscription subscription) {
        if (request.images() == null) {
            return;
        }

        boolean hasNonDetail = request.images().stream()
                .anyMatch(image -> image.imageType() != ImageType.DETAIL);
        if (hasNonDetail) {
            throw new BusinessException(CompanyErrorCode.GALLERY_IMAGE_TYPE_NOT_ALLOWED);
        }
    }
}
