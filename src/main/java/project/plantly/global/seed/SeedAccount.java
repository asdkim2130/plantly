package project.plantly.global.seed;

import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;

/**
 * 시드 계정 1건. 케이스 코드(U1/A1…)가 안정 키이고 userId 는 시드할 때마다 채워진다.
 * 매니페스트에 그대로 실려 프론트가 로그인에 쓴다.
 */
public record SeedAccount(
        String code,
        Long userId,
        String email,
        String password,
        String name,
        UserRole role,
        UserStatus status,
        String note
) {
}
