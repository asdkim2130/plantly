package project.plantly.domain.company.industry;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.industry.dto.IndustryAdminResponse;
import project.plantly.domain.company.industry.dto.IndustryCreateRequest;
import project.plantly.domain.company.industry.dto.IndustryCreateResponse;
import project.plantly.global.response.ApiResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class IndustryController {

    private final IndustryService industryService;

    @PostMapping("/api/v1/admin/industries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IndustryCreateResponse> createIndustry (@Valid @RequestBody IndustryCreateRequest request){

        Long id = industryService.createIndustry(request);
        return ApiResponse.success("산업군 생성이 완료되었습니다.", new IndustryCreateResponse(id));
    }

    @GetMapping("/api/v1/admin/industries")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<IndustryAdminResponse>> getAll (){

        List<IndustryAdminResponse> all = industryService.getAll();
        return ApiResponse.success(all);
    }
}
