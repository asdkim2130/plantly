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
// brandColor 슬라이스는 없다 — 등급 게이트를 쓰기 시점에서 걷어내고 스팟라이트 조회 쪽으로 옮겼다(GradePolicy 주석 참고).
public record CompanyPolicyView(
        Company company,
        CompanySubscription subscription,
        List<Long> categoryIds,          // null = 스킵 (CategoryLimitPolicy)
        List<ImageRequest> galleryImages,// null = 스킵 (DetailImageLimitPolicy / GalleryImageTypePolicy)
        List<ReferenceRequest> references,// null = 스킵 (ReferenceImagePolicy)
        String videoUrl                  // null/blank = 스킵 (VideoUrlPolicy)
) {

    // 등록: 전체 요청이 대상.
    public static CompanyPolicyView forCreate(Company company, CompanyCreateRequest request, CompanySubscription subscription) {
        return new CompanyPolicyView(company, subscription,
                request.categoryIds(), request.images(), request.references(), request.videoUrl());
    }

    // 수정(기본정보 PATCH): videoUrl 만 대상.
    public static CompanyPolicyView forBasicInfoUpdate(Company company, CompanySubscription subscription, String videoUrl) {
        return new CompanyPolicyView(company, subscription, null, null, null, videoUrl);
    }

    // 수정(카테고리 PUT): 새 카테고리 목록만 대상.
    public static CompanyPolicyView forCategoryUpdate(Company company, CompanySubscription subscription, List<Long> categoryIds) {
        return new CompanyPolicyView(company, subscription, categoryIds, null, null, null);
    }

    // 수정(갤러리 PUT): 새 갤러리 이미지 목록만 대상.
    public static CompanyPolicyView forGalleryUpdate(Company company, CompanySubscription subscription, List<ImageRequest> galleryImages) {
        return new CompanyPolicyView(company, subscription, null, galleryImages, null, null);
    }

    // 수정(레퍼런스 PUT): 새 레퍼런스 목록만 대상.
    public static CompanyPolicyView forReferenceUpdate(Company company, CompanySubscription subscription, List<ReferenceRequest> references) {
        return new CompanyPolicyView(company, subscription, null, null, references, null);
    }
}
