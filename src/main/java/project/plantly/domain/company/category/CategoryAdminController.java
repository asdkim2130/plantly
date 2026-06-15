package project.plantly.domain.company.category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.category.dto.CategoryCreateRequest;
import project.plantly.domain.company.category.dto.CategoryTreeResponse;
import project.plantly.global.response.ApiResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryAdminController {

    private final CategoryAdminService categoryAdminService;

    @PostMapping("/api/v1/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> create (@Valid @RequestBody CategoryCreateRequest request){

        return ApiResponse.success("카테고리 생성이 완료되었습니다.", categoryAdminService.create(request));

    }

    // 관리자용 카테고리 조회
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/categories")
    public ApiResponse<List<CategoryTreeResponse>> getTree (){

        List<CategoryTreeResponse> tree = categoryAdminService.getTree();

        return ApiResponse.success(tree);
    }



}
