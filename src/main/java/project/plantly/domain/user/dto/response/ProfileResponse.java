package project.plantly.domain.user.dto.response;

import lombok.Builder;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;

import java.time.LocalDateTime;

@Builder
public record ProfileResponse(
        String email,
        String name,
        String nickname,
        String phone,
        UserStatus userStatus,
        // 권한. 화면이 관리자 메뉴(관리자 기업 목록 등)를 보일지 정하는 근거다 — 접근 제어 자체는 서버의 @PreAuthorize 가 한다.
        UserRole userRole,
        LocalDateTime createdAt
) {

    public static ProfileResponse from (User user){

        return ProfileResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .userStatus(user.getUserStatus())
                .userRole(user.getUserRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
