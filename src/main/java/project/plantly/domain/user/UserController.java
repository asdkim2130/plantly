package project.plantly.domain.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.user.dto.request.SignUpRequest;
import project.plantly.domain.user.dto.response.ProfileResponse;
import project.plantly.global.response.ApiResponse;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입
    @PostMapping("/api/v1/users/sign-up")
    public ApiResponse signUp (@Valid @RequestBody SignUpRequest request){

        userService.createUser(request);

        return ApiResponse.success("회원가입이 완료되었습니다.");
    }

    // 회원 자신의 프로필 조회
    @GetMapping("/api/v1/users/me")
    public ProfileResponse getProfile (Long userId){

        return userService.getUserProfile(userId);
    }

}
