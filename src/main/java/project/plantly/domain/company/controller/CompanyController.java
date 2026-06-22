package project.plantly.domain.company.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.service.CompanyService;
import project.plantly.global.response.ApiResponse;
import project.plantly.global.response.IdResponse;
import project.plantly.global.security.UserPrincipal;

@RestController
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    // 유저 자가등록 — 인증된 본인이 소유자가 된다.
    @PostMapping("/api/v1/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IdResponse> createMyCompany(@AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody CompanyCreateRequest request) {

        Long id = companyService.createByUser(
                principal.getUser().getId(), principal.getUser().getUserGrade(), request);
        return ApiResponse.success("회사 등록이 완료되었습니다.", new IdResponse(id));
    }
}
