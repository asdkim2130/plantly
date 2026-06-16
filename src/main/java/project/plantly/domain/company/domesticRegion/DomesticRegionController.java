package project.plantly.domain.company.domesticRegion;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.domesticRegion.dto.DomesticRegionAdminResponse;
import project.plantly.global.response.ApiResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DomesticRegionController {

    private final DomesticRegionService domesticRegionService;

    @GetMapping("/api/v1/admin/domestic-regions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<DomesticRegionAdminResponse>> getAll (){

        return ApiResponse.success(domesticRegionService.getTree());
    }


}
