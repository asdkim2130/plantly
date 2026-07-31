package project.plantly.domain.company.domesticRegion;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.domesticRegion.dto.DomesticRegionAdminResponse;
import project.plantly.domain.company.domesticRegion.dto.DomesticRegionPublicResponse;
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

    /**
     * 공개 국내 지역 옵션 트리. 회사 등록/수정 폼의 커버리지 선택 소스로, 비로그인 사용자도
     * 폼 이전 화면을 둘러보므로 인증을 요구하지 않는다(SecurityConfig 에서 permitAll).
     *
     * <p>관리자 트리와 달리 활성 지역만 내려주고, {@code id} 와 표기 2종을 함께 준다.
     * 등록 요청이 {@code domesticRegionIds} 로 id 를 받기 때문이다.
     *
     * <p>루트는 전국 + 시도 17개이며 순서는 code 순이라 전국이 항상 첫 항목이다.
     * 자식이 없는 루트(전국·광역시 8곳)는 1차에서 바로 확정되고, 자식이 있는 도 9곳만
     * 2차 드롭다운이 열린다 — 그 첫 항목 '전역'에는 도 자신의 id 를 쓴다.
     */
    @GetMapping("/api/v1/domestic-regions")
    public ApiResponse<List<DomesticRegionPublicResponse>> getPublicTree (){

        return ApiResponse.success(domesticRegionService.getPublicTree());
    }


}
