package project.plantly.domain.company.dto;

import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.enums.SubscriptionStatus;

import java.time.LocalDate;

// 어떤 유저가 '소유(OWNER)'한 회사의 구독 요약. 관리자 유저 목록의 등급 배지(연착륙)에 쓰인다.
//
// 구독의 주인은 회사이므로 저장/편집은 회사 구독으로 이뤄지고, 이 요약은 유저 관점에서 파생해 보여주기만 하는
// 읽기 전용 뷰다. companyId 를 함께 담아 프론트가 배지 옆 수정 버튼을 회사 구독 화면으로 연결할 수 있게 한다.
// effectiveGrade 는 엔티티 규칙을 그대로 재사용(만료/체험 반영)해 파생한다.
public record OwnerSubscriptionSummary(
        Long companyId,
        CompanyGrade effectiveGrade,
        SubscriptionStatus status,
        LocalDate expiresAt
) {

    public static OwnerSubscriptionSummary from(CompanySubscription subscription) {
        return new OwnerSubscriptionSummary(
                subscription.getCompanyId(),
                subscription.effectiveGrade(),
                subscription.getStatus(),
                subscription.getExpiresAt());
    }
}
