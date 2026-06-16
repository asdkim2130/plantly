package project.plantly.domain.company.certification;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.certification.dto.CertificationCreateRequest;
import project.plantly.global.response.ApiResponse;

@RestController
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/certifications")
    public ApiResponse<Long> createCertification (@Valid @RequestBody CertificationCreateRequest request){

        return ApiResponse.success("인증 항목이 등록되었습니다.", certificationService.createCertification(request));
    }
}
