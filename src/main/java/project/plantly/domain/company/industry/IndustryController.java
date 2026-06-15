package project.plantly.domain.company.industry;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.industry.dto.IndustryAdminResponse;
import project.plantly.domain.company.industry.dto.IndustryCreateRequest;
import project.plantly.global.response.ApiResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class IndustryController {

    private final IndustryService industryService;

    @PostMapping("/api/v1/admin/industries")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> createIndustry (@Valid @RequestBody IndustryCreateRequest request){

        return ApiResponse.success("산업군 생성이 완료되었습니다.", industryService.createIndustry(request));
    }

    @GetMapping("/api/v1/admin/industries")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<IndustryAdminResponse>> getAll (){

        List<IndustryAdminResponse> all = industryService.getAll();
        return ApiResponse.success(all);
    }
}
