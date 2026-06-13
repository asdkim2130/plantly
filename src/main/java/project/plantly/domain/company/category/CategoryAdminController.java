package project.plantly.domain.company.category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.category.dto.CategoryCreateRequest;
import project.plantly.domain.company.category.tree.CategoryTreeService;
import project.plantly.global.response.ApiResponse;

@RestController
@RequiredArgsConstructor
public class CategoryAdminController {

    private final CategoryAdminService categoryAdminService;
    private final CategoryTreeService categoryTreeService;

    @PostMapping("/api/v1/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> create (@Valid @RequestBody CategoryCreateRequest request){

        return ApiResponse.success("카테코리 생성이 완료되었습니다.", categoryAdminService.create(request));

    }



}
