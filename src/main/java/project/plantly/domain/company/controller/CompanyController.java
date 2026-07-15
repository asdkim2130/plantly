package project.plantly.domain.company.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ContactRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ImageRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ReferenceRequest;
import project.plantly.domain.company.dto.CompanyDetailResponse;
import project.plantly.domain.company.dto.CompanyPublicResponse;
import project.plantly.domain.company.dto.CompanySubscriptionResponse;
import project.plantly.domain.company.dto.CompanyUpdateRequest;
import project.plantly.domain.company.dto.CompanyVisibilityUpdateRequest;
import project.plantly.domain.company.search.dto.CompanySearchRequest;
import project.plantly.domain.company.search.dto.CompanySummary;
import project.plantly.domain.company.service.CompanyQueryService;
import project.plantly.domain.company.service.CompanyService;
import project.plantly.domain.company.service.CompanyUpdateService;
import project.plantly.global.PageResponse;
import project.plantly.global.response.ApiResponse;
import project.plantly.global.response.IdResponse;
import project.plantly.global.security.UserPrincipal;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyQueryService companyQueryService;
    private final CompanyUpdateService companyUpdateService;

    // 유저 자가등록 — 인증된 본인이 소유자가 된다.
    @PostMapping("/api/v1/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IdResponse> createMyCompany(@AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody CompanyCreateRequest request) {

        Long id = companyService.createByUser(principal.getUser().getId(), request);
        return ApiResponse.success("회사 등록이 완료되었습니다.", new IdResponse(id));
    }

    // 공개 회사 목록/검색 — 인증 없이 누구나. 통합 키워드 + 고급검색 + 패싯(인증/산업군/카테고리 서브트리).
    // 기본 정렬(spotlight→featured→최신)으로 페이징된 요약 카드를 반환한다.
    @GetMapping("/api/v1/companies")
    public ApiResponse<PageResponse<CompanySummary>> searchCompanies(@AuthenticationPrincipal UserPrincipal principal,
                                                                     @ModelAttribute CompanySearchRequest request,
                                                                     @PageableDefault(size = 20) Pageable pageable) {
        // 인증은 선택: 로그인 상태면 카드마다 좋아요/즐겨찾기 여부를 채우고, 익명이면 principal=null → 전부 false.
        Long viewerId = (principal == null) ? null : principal.getUser().getId();
        return ApiResponse.success(companyQueryService.search(request.toCriteria(), pageable, viewerId));
    }

    // 내가 등록한 회사 목록 — 인증된 본인 소유(userId=본인) 미삭제 회사를 요약 카드로, 최신순 페이징(검색 없음).
    // 단일 세그먼트 'my' 라 공개 상세(/{id})보다 먼저 매칭된다(SecurityConfig 에서도 /{id} permitAll 앞에 인증 규칙을 둔다).
    @GetMapping("/api/v1/companies/my")
    public ApiResponse<PageResponse<CompanySummary>> getMyCompanies(@AuthenticationPrincipal UserPrincipal principal,
                                                                    @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(companyQueryService.listMyCompanies(principal.getUser().getId(), pageable));
    }

    // 내 즐겨찾기 회사 목록 — 인증된 본인이 즐겨찾기한 미삭제 회사를 요약 카드로, 즐겨찾기순(담은 최신순) 페이징.
    // 검색/패싯 없음: 즐겨찾기는 회사 속성이 아니라 뷰어↔회사 관계라 공개 검색(GET /companies)의 조건으로 넣지 않고
    // 별도 경로로 둔다(정렬 키 f.created_at 도 검색 쿼리엔 없다). 'my' 와 같은 단일 세그먼트라 /{id} 보다 먼저 매칭된다.
    @GetMapping("/api/v1/companies/favorites")
    public ApiResponse<PageResponse<CompanySummary>> getMyFavorites(@AuthenticationPrincipal UserPrincipal principal,
                                                                    @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(companyQueryService.listMyFavorites(principal.getUser().getId(), pageable));
    }

    // 일반(공개) 상세 조회 — 누구에게나 안전한 공개 필드만 반환한다. 소프트 삭제된 회사는 404.
    // 인증은 선택: 로그인 상태면 principal 로 좋아요/즐겨찾기 여부(likedByMe/favoritedByMe)를 채우고, 익명이면 principal=null → false.
    @GetMapping("/api/v1/companies/{id}")
    public ApiResponse<CompanyPublicResponse> getCompany(@AuthenticationPrincipal UserPrincipal principal,
                                                         @PathVariable Long id) {
        Long viewerId = (principal == null) ? null : principal.getUser().getId();
        return ApiResponse.success(companyQueryService.getPublic(id, viewerId));
    }

    // 소유자 전용 상세 조회 — 요청자가 해당 회사의 멤버여야 하며, 내부·운영 메타(meta)까지 포함한다.
    @GetMapping("/api/v1/companies/{id}/private")
    public ApiResponse<CompanyDetailResponse> getMyCompany(@AuthenticationPrincipal UserPrincipal principal,
                                                           @PathVariable Long id) {
        return ApiResponse.success(companyQueryService.getOwnerView(id, principal.getUser().getId()));
    }

    // 소유자 전용 구독 조회 — 요청자가 해당 회사의 멤버여야 한다. 회사 데이터와 섞지 않고 구독 정보만 단독으로 반환한다.
    @GetMapping("/api/v1/companies/{id}/subscription")
    public ApiResponse<CompanySubscriptionResponse> getMySubscription(@AuthenticationPrincipal UserPrincipal principal,
                                                                      @PathVariable Long id) {
        return ApiResponse.success(companyQueryService.getSubscriptionForOwner(id, principal.getUser().getId()));
    }

    // 기본 정보 부분 수정 — 소유자만. null=미변경(sparse). 수정 값은 화면에 이미 반영되므로 본문 없이 성공만 반환한다.
    @PatchMapping("/api/v1/companies/{id}")
    public ApiResponse<Void> updateMyCompany(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long id,
                                             @Valid @RequestBody CompanyUpdateRequest request) {
        companyUpdateService.updateBasicInfoByUser(id, principal.getUser().getId(), request);
        return ApiResponse.ok();
    }

    // 공개/비공개 전환 — 소유자만. 목표 상태(PUBLIC/PRIVATE)를 지정한다(멱등). 응답은 본문 없이 성공만.
    @PatchMapping("/api/v1/companies/{id}/visibility")
    public ApiResponse<Void> changeVisibility(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long id,
                                              @Valid @RequestBody CompanyVisibilityUpdateRequest request) {
        companyUpdateService.changeVisibilityByUser(id, principal.getUser().getId(), request.visibility());
        return ApiResponse.ok();
    }

    // ===== 컬렉션 전체 교체(PUT) — 소유자만. 각 컬렉션을 통째로 새 리스트로 교체한다(빈 리스트 = 전부 비우기). =====
    // 표시 순서(displayOrder)는 클라이언트가 보내지 않고 서버가 리스트 인덱스로 재부여한다. 응답은 본문 없이 성공만.

    @PutMapping("/api/v1/companies/{id}/tags")
    public ApiResponse<Void> replaceTags(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long id, @RequestBody List<String> tagNames) {
        companyUpdateService.replaceTagsByUser(id, principal.getUser().getId(), tagNames);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/materials")
    public ApiResponse<Void> replaceMaterials(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long id, @RequestBody List<String> materialNames) {
        companyUpdateService.replaceMaterialsByUser(id, principal.getUser().getId(), materialNames);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/equipment")
    public ApiResponse<Void> replaceEquipment(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long id, @RequestBody List<String> equipmentNames) {
        companyUpdateService.replaceEquipmentByUser(id, principal.getUser().getId(), equipmentNames);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/images")
    public ApiResponse<Void> replaceGalleryImages(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable Long id, @Valid @RequestBody List<ImageRequest> images) {
        companyUpdateService.replaceGalleryImagesByUser(id, principal.getUser().getId(), images);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/contacts")
    public ApiResponse<Void> replaceContacts(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long id, @Valid @RequestBody List<ContactRequest> contacts) {
        companyUpdateService.replaceContactsByUser(id, principal.getUser().getId(), contacts);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/references")
    public ApiResponse<Void> replaceReferences(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable Long id, @Valid @RequestBody List<ReferenceRequest> references) {
        companyUpdateService.replaceReferencesByUser(id, principal.getUser().getId(), references);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/categories")
    public ApiResponse<Void> replaceCategories(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable Long id, @RequestBody List<Long> categoryIds) {
        companyUpdateService.replaceCategoriesByUser(id, principal.getUser().getId(), categoryIds);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/industries")
    public ApiResponse<Void> replaceIndustries(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable Long id, @RequestBody List<Long> industryIds) {
        companyUpdateService.replaceIndustriesByUser(id, principal.getUser().getId(), industryIds);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/certifications")
    public ApiResponse<Void> replaceCertifications(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long id, @RequestBody List<Long> certificationIds) {
        companyUpdateService.replaceCertificationsByUser(id, principal.getUser().getId(), certificationIds);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/countries")
    public ApiResponse<Void> replaceCountries(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long id, @RequestBody List<Long> countryIds) {
        companyUpdateService.replaceCountriesByUser(id, principal.getUser().getId(), countryIds);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/regions")
    public ApiResponse<Void> replaceRegions(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable Long id, @RequestBody List<Long> domesticRegionIds) {
        companyUpdateService.replaceRegionsByUser(id, principal.getUser().getId(), domesticRegionIds);
        return ApiResponse.ok();
    }
}
