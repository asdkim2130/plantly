package project.plantly.domain.user.dto.response;

import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;

import java.time.LocalDateTime;

// 관리자 유저 목록의 '유저 본체' 투영(내부용). userId 를 실어, 서비스가 소유 회사 구독을 배치로 붙일 때 상관키로 쓴다.
// 최종 응답(AdminUserListResponse)에는 userId 대신 구독 요약이 병합된다.
public record AdminUserRow(
        Long userId,
        String email,
        String name,
        String phone,
        LocalDateTime createdAt,
        UserRole userRole,
        UserStatus userStatus
) {
}
