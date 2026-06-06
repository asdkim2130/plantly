package project.plantly.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 10, max = 60, message = "비밀번호는 최소 10자 최대 60자입니다.")
        @Pattern(regexp = "^(?=.*[^A-Za-z0-9]).+$", message = "비밀번호에 특수문자를 최소 1개 포함해야 합니다.")
        String password,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 30, message = "이름은 최소 2자 최대 30자입니다.")
        @Pattern(regexp = "^[a-zA-Z가-힣]+$", message = "이름은 한글 또는 영문만 입력 가능합니다.")
        String name,

        @NotBlank(message = "휴대폰 번호는 필수입니다.")
        @Size(min = 10, max = 11, message = "휴대폰 번호는 10자 또는 11자입니다.")
        @Pattern(regexp = "^[0-9]+$", message = "휴대폰 번호는 숫자만 입력 가능합니다.")
        String phone
) {
}