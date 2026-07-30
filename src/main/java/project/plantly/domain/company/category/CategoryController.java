package project.plantly.domain.company.category;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.category.dto.CategoryPublicResponse;
import project.plantly.global.response.ApiResponse;

import java.util.List;

/**
 * 카테고리 공개 조회 전용. 쓰기/관리자 조회는 {@link CategoryAdminController} 에 있다.
 */
@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 공개 카테고리 트리. 회사 등록 폼의 대→중→소 3단 선택과 검색 필터 패널의 소스로, 비로그인
     * 사용자도 검색 화면을 쓰므로 인증을 요구하지 않는다(SecurityConfig 에서 permitAll).
     *
     * <p>검색의 카테고리 패싯(categoryIds)에는 어느 depth 의 id 든 실어 보낼 수 있다 —
     * 서버가 조상 closure({@code company_category_closure})로 매칭하므로 대분류를 고르면
     * 그 아래 소분류만 등록한 회사도 잡힌다.
     */
    @GetMapping("/api/v1/categories")
    public ApiResponse<List<CategoryPublicResponse>> getPublicTree (){

        return ApiResponse.success(categoryService.getPublicTree());
    }
}
