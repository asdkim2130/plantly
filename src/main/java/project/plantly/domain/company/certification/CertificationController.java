package project.plantly.domain.company.certification;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.plantly.domain.company.certification.dto.CertificationAdminResponse;
import project.plantly.domain.company.certification.dto.CertificationCreateRequest;
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
}
