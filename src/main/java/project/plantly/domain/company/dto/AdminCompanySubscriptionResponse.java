package project.plantly.domain.company.dto;

import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 관리자용 구독 조회 응답. 사용자용(CompanySubscriptionResponse)과 같은 구독 사실에
// 감사용 타임스탬프(createdAt/updatedAt)를 더한다 — 관리자가 수정 이력(마지막 변경 시각 등)을 가늠하는 데 쓴다.
// grade(계약)와 effectiveGrade(지금 유효 등급)를 함께 내려 체험/만료로 인한 강등을 오해 없이 판단하게 한다.
public record AdminCompanySubscriptionResponse(
        Long companyId,
        String companyName,
        CompanyGrade grade,
        CompanyGrade effectiveGrade,
        SubscriptionStatus status,
        LocalDate startedAt,
        LocalDate expiresAt,         // null = 무기한(만료 없음)
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AdminCompanySubscriptionResponse from(CompanySubscription subscription, String companyName) {
        return new AdminCompanySubscriptionResponse(
                subscription.getCompanyId(),
                companyName,
                subscription.getGrade(),
                subscription.effectiveGrade(),
                subscription.getStatus(),
                subscription.getStartedAt(),
                subscription.getExpiresAt(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt());
    }
}
