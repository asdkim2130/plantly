package project.plantly.domain.company.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import project.plantly.domain.company.dto.CompanyUpdateRequest;
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

    // ===== 관리자 수정 — 소유 무관, 모든 회사 대상. 유저 수정과 동일한 변경/구조 불변식을 재사용한다. =====
    // (등급 한도·변형 정책은 유저 경로와 동일하게 아직 미적용 — CompanyUpdateService 의 TODO 참고)

    @PatchMapping("/api/v1/admin/companies/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateCompanyByAdmin(@PathVariable Long id,
                                                  @Valid @RequestBody CompanyUpdateRequest request) {
        companyUpdateService.updateBasicInfoByAdmin(id, request);
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
