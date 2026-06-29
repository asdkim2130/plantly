package project.plantly.domain.company.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.dto.CompanyDetailResponse;
import project.plantly.domain.company.dto.CompanyPublicResponse;
import project.plantly.domain.company.search.dto.CompanySearchRequest;
import project.plantly.domain.company.search.dto.CompanySummary;
import project.plantly.domain.company.service.CompanyQueryService;
import project.plantly.domain.company.service.CompanyService;
import project.plantly.global.PageResponse;
import project.plantly.global.response.ApiResponse;
import project.plantly.global.response.IdResponse;
import project.plantly.global.security.UserPrincipal;

@RestController
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyQueryService companyQueryService;

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
    public ApiResponse<PageResponse<CompanySummary>> searchCompanies(@ModelAttribute CompanySearchRequest request,
                                                                     @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(companyQueryService.search(request.toCriteria(), pageable));
    }

    // 일반(공개) 상세 조회 — 누구에게나 안전한 공개 필드만 반환한다. 소프트 삭제된 회사는 404.
    @GetMapping("/api/v1/companies/{id}")
    public ApiResponse<CompanyPublicResponse> getCompany(@PathVariable Long id) {
        return ApiResponse.success(companyQueryService.getPublic(id));
    }

    // 소유자 전용 상세 조회 — 요청자가 해당 회사의 멤버여야 하며, 내부·운영 메타(meta)까지 포함한다.
    @GetMapping("/api/v1/companies/{id}/private")
    public ApiResponse<CompanyDetailResponse> getMyCompany(@AuthenticationPrincipal UserPrincipal principal,
                                                           @PathVariable Long id) {
        return ApiResponse.success(companyQueryService.getOwnerView(id, principal.getUser().getId()));
    }
}
