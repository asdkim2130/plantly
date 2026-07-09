package project.plantly.domain.company.stat;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import project.plantly.domain.company.stat.dto.FavoriteToggleResponse;
import project.plantly.domain.company.stat.dto.LikeToggleResponse;
import project.plantly.global.response.ApiResponse;
import project.plantly.global.security.UserPrincipal;

// 유저의 회사 좋아요/즐겨찾기 토글. 인증 필수(SecurityConfig anyRequest().authenticated()).
// 멀티 세그먼트 POST 라 공개 GET /{id} 매처와 충돌하지 않는다. 응답은 토글 후 상태 + 갱신 카운트.
@RestController
@RequiredArgsConstructor
public class CompanyStatController {

    private final CompanyStatService companyStatService;

    // 좋아요 토글 — 눌려있지 않으면 등록, 눌려있으면 취소. 응답 liked 로 현재 상태를 알려준다.
    @PostMapping("/api/v1/companies/{id}/like")
    public ApiResponse<LikeToggleResponse> toggleLike(@AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable Long id) {
        return ApiResponse.success(companyStatService.toggleLike(principal.getUser().getId(), id));
    }

    // 즐겨찾기 토글 — 좋아요와 동일한 on/off. 목록 조회는 별도 엔드포인트(추후)가 담당한다.
    @PostMapping("/api/v1/companies/{id}/favorite")
    public ApiResponse<FavoriteToggleResponse> toggleFavorite(@AuthenticationPrincipal UserPrincipal principal,
                                                              @PathVariable Long id) {
        return ApiResponse.success(companyStatService.toggleFavorite(principal.getUser().getId(), id));
    }
}
