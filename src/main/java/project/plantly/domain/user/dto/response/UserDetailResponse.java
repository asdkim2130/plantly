package project.plantly.domain.user.dto.response;

import lombok.Builder;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserGrade;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;

import java.time.LocalDateTime;

@Builder
public record UserDetailResponse(Long id,
                                 String email,
                                 String name,
                                 String nickname,
                                 String phone,
                                 UserStatus userStatus,
                                 UserGrade userGrade,
                                 UserRole userRole,
                                 LocalDateTime trialEndDate,
                                 LocalDateTime createdAt,
                                 LocalDateTime updatedAt,
                                 LocalDateTime deletedAt) {

    public static UserDetailResponse from (User user){
        return UserDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .userStatus(user.getUserStatus())
                .userGrade(user.getUserGrade())
                .userRole(user.getUserRole())
                .trialEndDate(user.getTrialEndDate())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .build();
    }
}