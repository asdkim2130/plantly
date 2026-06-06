package project.plantly.domain.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 30, message = "이름은 최소 2자 최대 30자입니다.")
        @Pattern(regexp = "^[a-zA-Z가-힣]+$", message = "이름은 한글 또는 영문만 입력 가능합니다.")
        String name,

        @Size(min = 2, max = 15)
        String nickname,

        @Size(min = 10, max = 11, message = "휴대폰 번호는 10자 또는 11자입니다.")
        @Pattern(regexp = "^[0-9]+$", message = "휴대폰 번호는 숫자만 입력 가능합니다.")
        String phone

) {
}
