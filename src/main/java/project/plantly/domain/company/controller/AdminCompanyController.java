package project.plantly.domain.company.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.dto.CompanyDetailResponse;
import project.plantly.domain.company.service.CompanyQueryService;
import project.plantly.domain.company.service.CompanyService;
import project.plantly.global.response.ApiResponse;
import project.plantly.global.response.IdResponse;
import project.plantly.global.security.UserPrincipal;

@RestController
@RequiredArgsConstructor
public class AdminCompanyController {

    private final CompanyService companyService;
    private final CompanyQueryService companyQueryService;

    // 관리자 등록 — 소유자 미연동(userId=null) 상태로 생성. registeredBy 에 등록한 관리자 id 기록.
    @PostMapping("/api/v1/admin/companies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IdResponse> createCompanyByAdmin(@AuthenticationPrincipal UserPrincipal principal,
                                                        @Valid @RequestBody CompanyCreateRequest request) {

        Long id = companyService.createByAdmin(principal.getUser().getId(), request);
        return ApiResponse.success("회사 등록이 완료되었습니다.", new IdResponse(id));
    }

    // 관리자 상세 조회 — 상태(삭제/미연동 등) 무관하게 전체를 본다. 소유자 뷰와 동일한 상세 응답을 재사용한다.
    @GetMapping("/api/v1/admin/companies/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CompanyDetailResponse> getCompanyByAdmin(@PathVariable Long id) {
        return ApiResponse.success(companyQueryService.getForAdmin(id));
    }
}
