package project.plantly.domain.user.dto.response;

import lombok.Builder;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserStatus;

import java.time.LocalDateTime;

@Builder
public record ProfileResponse(
        String email,
        String name,
        String nickname,
        String phone,
        UserStatus userStatus,
        LocalDateTime createdAt
) {

    public static ProfileResponse from (User user){

        return ProfileResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .userStatus(user.getUserStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
