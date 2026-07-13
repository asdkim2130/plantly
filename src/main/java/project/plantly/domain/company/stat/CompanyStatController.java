package project.plantly.domain.company.stat;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.global.security.UserPrincipal;

// 유저의 회사 좋아요/즐겨찾기. 인증 필수(SecurityConfig anyRequest().authenticated()).
// 토글 대신 등록(PUT)/해제(DELETE)로 동사를 분리해 멱등하다 — 재요청·중복클릭에도 상태가 한 방향으로만 수렴한다.
// 응답은 항상 204 No Content: 이미 원하는 상태(좋아요 켜짐/꺼짐)에 도달한 재요청은 오류가 아니므로 409 가 아닌 204 로 처리한다.
// 현재 상태(likedByMe/favoritedByMe)는 조회 API 가 이미 내려주므로 본문으로 되돌려줄 필요가 없다.
@RestController
@RequiredArgsConstructor
public class CompanyStatController {

    private final CompanyStatService companyStatService;

    // 좋아요 등록(멱등). 이미 좋아요 중이어도 204.
    @PutMapping("/api/v1/companies/{id}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void like(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        companyStatService.like(principal.getUser().getId(), id);
    }

    // 좋아요 해제(멱등). 좋아요가 없어도 204.
    @DeleteMapping("/api/v1/companies/{id}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlike(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        companyStatService.unlike(principal.getUser().getId(), id);
    }

    // 즐겨찾기 등록(멱등). 목록 조회는 별도 엔드포인트(추후)가 담당한다.
    @PutMapping("/api/v1/companies/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void favorite(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        companyStatService.favorite(principal.getUser().getId(), id);
    }

    // 즐겨찾기 해제(멱등).
    @DeleteMapping("/api/v1/companies/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfavorite(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        companyStatService.unfavorite(principal.getUser().getId(), id);
    }
}