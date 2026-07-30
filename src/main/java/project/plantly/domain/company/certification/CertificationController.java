package project.plantly.domain.company.certification;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.plantly.domain.company.certification.dto.CertificationAdminResponse;
import project.plantly.domain.company.certification.dto.CertificationCreateRequest;
import project.plantly.domain.company.certification.dto.CertificationPublicResponse;
import project.plantly.global.response.ApiResponse;
import project.plantly.global.response.IdResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/certifications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IdResponse> createCertification (@Valid @RequestBody CertificationCreateRequest request){

        Long id = certificationService.createCertification(request);
        return ApiResponse.success("인증 항목이 등록되었습니다.", new IdResponse(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/certifications")
    public ApiResponse<List<CertificationAdminResponse>> getAll (){

        return ApiResponse.success(certificationService.getAll());
    }

    /**
     * 공개 인증 옵션 목록. 회사 등록 폼과 검색 필터 패널의 드롭다운 소스로, 비로그인 사용자도 검색 화면을
     * 쓰므로 인증을 요구하지 않는다(SecurityConfig 에서 permitAll).
     *
     * <p>평면 목록이며 type 별 그룹핑은 프론트가 한다. 검색의 인증 패싯도 이 id 를 평면 리스트로 받는다
     * (그룹 간 AND/그룹 내 OR 판정은 서버가 type 을 보고 처리 —
     * {@code PostgresTrigramCompanySearch#appendCertificationFacet}).
     */
    @GetMapping("/api/v1/certifications")
    public ApiResponse<List<CertificationPublicResponse>> getPublicList (){

        return ApiResponse.success(certificationService.getPublicList());
    }
}
