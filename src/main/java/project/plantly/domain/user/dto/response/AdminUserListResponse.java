package project.plantly.domain.user.dto.response;

import project.plantly.domain.company.dto.OwnerSubscriptionSummary;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;

import java.time.LocalDateTime;

// subscription 은 이 유저가 '소유(OWNER)한 회사의 구독 요약'을 파생해 곁들인 배지다(연착륙). 회사를 소유하지 않은
// 유저는 null 이다. 구독의 주인은 회사이므로 이 값은 읽기 전용이며, 편집은 회사 구독(companyId)으로 이뤄진다.
public record AdminUserListResponse(String email,
                                    String name,
                                    String phone,
                                    LocalDateTime createdAt,
                                    UserRole userRole,
                                    UserStatus userStatus,
                                    OwnerSubscriptionSummary subscription
) {

    public static AdminUserListResponse of(AdminUserRow row, OwnerSubscriptionSummary subscription) {
        return new AdminUserListResponse(
                row.email(), row.name(), row.phone(), row.createdAt(), row.userRole(), row.userStatus(), subscription);
    }
}
