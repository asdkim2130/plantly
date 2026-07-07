package project.plantly.domain.company.policy;

import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ImageRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ReferenceRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanySubscription;

import java.util.List;

// 정책이 읽는 것만 추린 통합 입력. 등록(create)과 수정(update)이 같은 정책 코드를 공유하기 위한 seam.
//
// 각 슬라이스는 "이번 연산의 대상"만 채우고 나머지는 null 로 둔다 → 해당 정책이 스스로 스킵한다(create 의 null-skip 규약과 동일).
//  - create: 전체 요청이 대상이므로 모든 슬라이스를 채운다(forCreate).
//  - update: 바뀐 컬렉션/필드 하나만 채운다(forXxxUpdate) → 그 슬라이스를 읽는 정책만 발화한다(delta 검증).
//
// brandColor 는 값이 아니라 "이 연산이 brandColor 를 다루는가"(brandColorInScope)로 표현한다.
// (BrandColorPolicy 는 미허용 등급이면 기본값으로 '고정'만 하고 값을 읽지 않으므로, 값이 아닌 적용 여부만 알면 된다.)
//  - create: 항상 true (등록 시 미허용 등급은 색 미입력이어도 기본값으로 고정한다 — 기존 동작 유지).
//  - update: 요청에 brandColor 가 담겼을 때만 true (안 건드린 색은 그대로 둔다).
public record CompanyPolicyView(
        Company company,
        CompanySubscription subscription,
        List<Long> categoryIds,          // null = 스킵 (CategoryLimitPolicy)
        List<ImageRequest> galleryImages,// null = 스킵 (DetailImageLimitPolicy / GalleryImageTypePolicy)
        List<ReferenceRequest> references,// null = 스킵 (ReferenceImagePolicy)
        String videoUrl,                 // null/blank = 스킵 (VideoUrlPolicy)
        boolean brandColorInScope        // BrandColorPolicy 적용 여부
) {

    // 등록: 전체 요청이 대상. brandColor 는 항상 적용.
    public static CompanyPolicyView forCreate(Company company, CompanyCreateRequest request, CompanySubscription subscription) {
        return new CompanyPolicyView(company, subscription,
                request.categoryIds(), request.images(), request.references(), request.videoUrl(), true);
    }

    // 수정(기본정보 PATCH): videoUrl / brandColor 만 대상. brandColor 는 요청에 담겼을 때만 적용.
    public static CompanyPolicyView forBasicInfoUpdate(Company company, CompanySubscription subscription,
                                                       String videoUrl, boolean brandColorProvided) {
        return new CompanyPolicyView(company, subscription, null, null, null, videoUrl, brandColorProvided);
    }

    // 수정(카테고리 PUT): 새 카테고리 목록만 대상.
    public static CompanyPolicyView forCategoryUpdate(Company company, CompanySubscription subscription, List<Long> categoryIds) {
        return new CompanyPolicyView(company, subscription, categoryIds, null, null, null, false);
    }

    // 수정(갤러리 PUT): 새 갤러리 이미지 목록만 대상.
    public static CompanyPolicyView forGalleryUpdate(Company company, CompanySubscription subscription, List<ImageRequest> galleryImages) {
        return new CompanyPolicyView(company, subscription, null, galleryImages, null, null, false);
    }

    // 수정(레퍼런스 PUT): 새 레퍼런스 목록만 대상.
    public static CompanyPolicyView forReferenceUpdate(Company company, CompanySubscription subscription, List<ReferenceRequest> references) {
        return new CompanyPolicyView(company, subscription, null, null, references, null, false);
    }
}
