package project.plantly.domain.user.dto.response;

import lombok.Builder;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserStatus;

import java.time.LocalDateTime;

@Builder
public record ProfileResponse(
        String email,
        String name,
        String phone,
        UserStatus userStatus,
        LocalDateTime trialEndDate,
        LocalDateTime createdAt
) {

    public static ProfileResponse form (User user){

        return ProfileResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .userStatus(user.getUserStatus())
                .trialEndDate(user.getTrialEndDate())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
