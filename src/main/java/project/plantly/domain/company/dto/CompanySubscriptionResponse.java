package project.plantly.domain.company.dto;

import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.enums.SubscriptionStatus;

import java.time.LocalDate;

// 회사 구독 단독 조회 응답. 다른 회사 데이터와 섞지 않고 구독 사실만 담는다.
//
// grade(계약 등급)와 effectiveGrade(지금 실제 유효한 등급)를 함께 내려준다:
// 체험(TRIAL)이나 만료된 유료 구독은 grade 는 그대로여도 effectiveGrade 는 FREE 로 강등되므로,
// grade 만 보여주면 "나는 ENTERPRISE 인데 왜 FREE 한도?" 같은 오해가 생긴다.
// 만료 판정은 서버 시계 기준이라 클라가 계산하지 않고 서버가 파생(effectiveGrade())해 내려준다.
public record CompanySubscriptionResponse(
        Long companyId,
        CompanyGrade grade,          // 계약(저장)된 등급
        CompanyGrade effectiveGrade, // 지금 유효한 등급 (만료/체험 반영, 정책이 실제로 참조하는 값)
        SubscriptionStatus status,
        LocalDate startedAt,
        LocalDate expiresAt          // null = 무기한(만료 없음)
) {

    public static CompanySubscriptionResponse from(CompanySubscription subscription) {
        return new CompanySubscriptionResponse(
                subscription.getCompanyId(),
                subscription.getGrade(),
                subscription.effectiveGrade(),
                subscription.getStatus(),
                subscription.getStartedAt(),
                subscription.getExpiresAt());
    }
}
