package project.plantly.domain.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.user.dto.request.SignUpRequest;
import project.plantly.domain.user.dto.response.ProfileResponse;
import project.plantly.global.response.ApiResponse;
import project.plantly.global.security.UserPrincipal;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입
    @PostMapping("/api/v1/users/sign-up")
    public ApiResponse<Void> signUp (@Valid @RequestBody SignUpRequest request){

        userService.createUser(request);

        return ApiResponse.success("회원가입이 완료되었습니다.");
    }

    // 회원 자신의 프로필 조회
    @GetMapping("/api/v1/users/me")
    public ApiResponse<ProfileResponse> getProfile (@AuthenticationPrincipal UserPrincipal principal){

        return ApiResponse.success(userService.getUserProfile(principal.getUser().getId()));
    }

}
