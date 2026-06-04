package project.plantly.domain.user.dto.response;

import lombok.Builder;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserGrade;
import project.plantly.domain.user.enums.UserStatus;

@Builder
public record LoginResponse(
        Long id,
        String email,
        String name,
        UserStatus userStatus,
        UserGrade userGrade
) {

    public static LoginResponse from (User user){

        return LoginResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .userStatus(user.getUserStatus())
                .userGrade(user.getUserGrade())
                .build();
    }
}
