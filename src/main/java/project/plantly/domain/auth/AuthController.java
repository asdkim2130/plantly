package project.plantly.domain.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.user.dto.request.LoginRequest;
import project.plantly.domain.user.dto.response.LoginResponse;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


//     SPA가 로그인/변경 요청 전에 CSRF 토큰을 받아가는 엔드포인트.
//     CsrfToken을 주입받아 반환하면, 토큰 값이 해석되며 XSRF-TOKEN 쿠키가 응답에 기록된다.
//     이후 FE는 쿠키 값을 읽어 X-XSRF-TOKEN 헤더로 전송한다.

    @GetMapping("/api/v1/auth/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    // 로그인 (세션 기반): 인증 성공 시 SecurityContext를 세션에 저장하고 사용자 정보를 응답
    @PostMapping("/api/v1/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        return authService.login(request, httpRequest, httpResponse);
    }

    // 로그아웃은 Spring Security LogoutFilter가 처리: POST /api/v1/auth/logout
}
