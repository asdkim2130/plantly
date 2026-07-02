package project.plantly.domain.auth.dto.response;

import lombok.Builder;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserStatus;

@Builder
public record LoginResponse(
        Long id,
        String email,
        String name,
        UserStatus userStatus
) {

    public static LoginResponse from (User user){

        return LoginResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .userStatus(user.getUserStatus())
                .build();
    }
}
