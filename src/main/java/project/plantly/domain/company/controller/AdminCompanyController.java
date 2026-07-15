package project.plantly.domain.company.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import project.plantly.domain.company.dto.AdminCompanyFlagsRequest;
import project.plantly.domain.company.dto.AdminCompanySubscriptionResponse;
import project.plantly.domain.company.dto.AdminSubscriptionUpdateRequest;
import project.plantly.domain.company.dto.CompanyDetailResponse;
import project.plantly.domain.company.dto.CompanyUpdateRequest;
import project.plantly.domain.company.dto.CompanyVisibilityUpdateRequest;
import project.plantly.domain.company.search.dto.AdminCompanySearchRequest;
import project.plantly.domain.company.search.dto.AdminCompanySummary;
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
public class AdminCompanyController {

    private final CompanyService companyService;
    private final CompanyQueryService companyQueryService;
    private final CompanyUpdateService companyUpdateService;

    // 관리자 등록 — 소유자 미연동(userId=null) 상태로 생성. registeredBy 에 등록한 관리자 id 기록.
    @PostMapping("/api/v1/admin/companies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IdResponse> createCompanyByAdmin(@AuthenticationPrincipal UserPrincipal principal,
                                                        @Valid @RequestBody CompanyCreateRequest request) {

        Long id = companyService.createByAdmin(principal.getUser().getId(), request);
        return ApiResponse.success("회사 등록이 완료되었습니다.", new IdResponse(id));
    }

    // 관리자 회사 목록 — 기본 전체(삭제 포함). verified/featured/spotlight/deleted(3-상태)·회사명·소유자 id 로
    // 교집합 필터링, 운영 필드 포함 카드로 최신 등록순 페이징. ('my' 같은 세그먼트 충돌은 admin 경로엔 없다)
    @GetMapping("/api/v1/admin/companies")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AdminCompanySummary>> listCompaniesByAdmin(@ModelAttribute AdminCompanySearchRequest request,
                                                                               @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(companyQueryService.listForAdmin(request.toCriteria(), pageable));
    }

    // 관리자 상세 조회 — 상태(삭제/미연동 등) 무관하게 전체를 본다. 소유자 뷰와 동일한 상세 응답을 재사용한다.
    @GetMapping("/api/v1/admin/companies/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CompanyDetailResponse> getCompanyByAdmin(@PathVariable Long id) {
        return ApiResponse.success(companyQueryService.getForAdmin(id));
    }

    // 관리자 구독 조회 — 소유 무관, 회사 데이터와 섞지 않고 구독 정보만(감사 타임스탬프 포함) 단독 반환한다.
    // 수정 팝업이 현재값으로 필드를 채우는 소스다.
    @GetMapping("/api/v1/admin/companies/{id}/subscription")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminCompanySubscriptionResponse> getSubscriptionByAdmin(@PathVariable Long id) {
        return ApiResponse.success(companyQueryService.getSubscriptionForAdmin(id));
    }

    // 관리자 구독 수정 — grade/status/expiresAt 를 관리자가 직접 지정(full-replace). 수정 값은 이미 팝업에
    // 반영되므로 본문 없이 성공만 반환한다. startedAt 은 팩트라 수정하지 않는다.
    @PatchMapping("/api/v1/admin/companies/{id}/subscription")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateSubscriptionByAdmin(@PathVariable Long id,
                                                       @Valid @RequestBody AdminSubscriptionUpdateRequest request) {
        companyUpdateService.updateSubscriptionByAdmin(id, request);
        return ApiResponse.ok();
    }

    // ===== 관리자 수정 — 소유 무관, 모든 회사 대상. 유저 수정과 동일한 변경/구조 불변식을 재사용한다. =====
    // (등급 한도·변형 정책은 유저 경로와 동일하게 아직 미적용 — CompanyUpdateService 의 TODO 참고)

    @PatchMapping("/api/v1/admin/companies/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateCompanyByAdmin(@PathVariable Long id,
                                                  @Valid @RequestBody CompanyUpdateRequest request) {
        companyUpdateService.updateBasicInfoByAdmin(id, request);
        return ApiResponse.ok();
    }

    // 공개/비공개 전환 — 관리자, 소유 무관. 목표 상태(PUBLIC/PRIVATE)를 지정한다(멱등).
    @PatchMapping("/api/v1/admin/companies/{id}/visibility")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> changeVisibilityByAdmin(@PathVariable Long id,
                                                     @Valid @RequestBody CompanyVisibilityUpdateRequest request) {
        companyUpdateService.changeVisibilityByAdmin(id, request.visibility());
        return ApiResponse.ok();
    }

    // 관리자 운영 플래그 조정 — 인증/추천/스팟라이트를 목표값으로 설정한다(sparse: null=미변경, 멱등). 소유 무관.
    // 응답은 본문 없이 성공만. (수정 값은 이미 화면에 반영되므로)
    @PatchMapping("/api/v1/admin/companies/{id}/flags")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> changeFlagsByAdmin(@PathVariable Long id,
                                                @RequestBody AdminCompanyFlagsRequest request) {
        companyUpdateService.changeFlagsByAdmin(id, request);
        return ApiResponse.ok();
    }

    // 관리자 삭제(소프트) — 소유 무관, 모든 회사 대상. 멱등.
    @DeleteMapping("/api/v1/admin/companies/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteCompanyByAdmin(@PathVariable Long id) {
        companyUpdateService.deleteByAdmin(id);
        return ApiResponse.ok();
    }

    // 관리자 복구 — 삭제된 회사를 다시 활성화한다(관리자 전용). 삭제된 사이 같은 사업자번호가 활성으로 재등록됐다면
    // 409(BUSINESS_NUMBER_TAKEN). 멱등(이미 활성이어도 성공).
    @PostMapping("/api/v1/admin/companies/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> restoreCompanyByAdmin(@PathVariable Long id) {
        companyUpdateService.restoreByAdmin(id);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/admin/companies/{id}/tags")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> replaceTagsByAdmin(@PathVariable Long id, @RequestBody List<String> tagNames) {
        companyUpdateService.replaceTagsByAdmin(id, tagNames);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/admin/companies/{id}/materials")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> replaceMaterialsByAdmin(@PathVariable Long id, @RequestBody List<String> materialNames) {
        companyUpdateService.replaceMaterialsByAdmin(id, materialNames);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/admin/companies/{id}/equipment")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> replaceEquipmentByAdmin(@PathVariable Long id, @RequestBody List<String> equipmentNames) {
        companyUpdateService.replaceEquipmentByAdmin(id, equipmentNames);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/admin/companies/{id}/images")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> replaceGalleryImagesByAdmin(@PathVariable Long id, @Valid @RequestBody List<ImageRequest> images) {
        companyUpdateService.replaceGalleryImagesByAdmin(id, images);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/admin/companies/{id}/contacts")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> replaceContactsByAdmin(@PathVariable Long id, @Valid @RequestBody List<ContactRequest> contacts) {
        companyUpdateService.replaceContactsByAdmin(id, contacts);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/admin/companies/{id}/references")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> replaceReferencesByAdmin(@PathVariable Long id, @Valid @RequestBody List<ReferenceRequest> references) {
        companyUpdateService.replaceReferencesByAdmin(id, references);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/admin/companies/{id}/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> replaceCategoriesByAdmin(@PathVariable Long id, @RequestBody List<Long> categoryIds) {
        companyUpdateService.replaceCategoriesByAdmin(id, categoryIds);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/admin/companies/{id}/industries")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> replaceIndustriesByAdmin(@PathVariable Long id, @RequestBody List<Long> industryIds) {
        companyUpdateService.replaceIndustriesByAdmin(id, industryIds);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/admin/companies/{id}/certifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> replaceCertificationsByAdmin(@PathVariable Long id, @RequestBody List<Long> certificationIds) {
        companyUpdateService.replaceCertificationsByAdmin(id, certificationIds);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/admin/companies/{id}/countries")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> replaceCountriesByAdmin(@PathVariable Long id, @RequestBody List<Long> countryIds) {
        companyUpdateService.replaceCountriesByAdmin(id, countryIds);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/admin/companies/{id}/regions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> replaceRegionsByAdmin(@PathVariable Long id, @RequestBody List<Long> domesticRegionIds) {
        companyUpdateService.replaceRegionsByAdmin(id, domesticRegionIds);
        return ApiResponse.ok();
    }
}
