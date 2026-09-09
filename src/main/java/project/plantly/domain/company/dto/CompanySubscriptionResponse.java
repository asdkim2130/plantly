package project.plantly.domain.company.dto;

import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.enums.SubscriptionStatus;
import project.plantly.domain.company.policy.GradePolicy;

import java.time.LocalDate;

// 회사 구독 단독 조회 응답. 다른 회사 데이터와 섞지 않고 구독 사실만 담는다.
//
// grade(계약 등급)와 effectiveGrade(지금 실제 유효한 등급)를 함께 내려준다:
// 체험(TRIAL)이나 만료된 유료 구독은 grade 는 그대로여도 effectiveGrade 는 FREE 로 강등되므로,
// grade 만 보여주면 "나는 ENTERPRISE 인데 왜 FREE 한도?" 같은 오해가 생긴다.
// 만료 판정은 서버 시계 기준이라 클라가 계산하지 않고 서버가 파생(effectiveGrade())해 내려준다.
public record CompanySubscriptionResponse(
        Long companyId,
        String companyName,          // 구독 주체(회사) 이름. 구독은 companyId 만 알아 호출부에서 함께 넘겨준다.
        CompanyGrade grade,          // 계약(저장)된 등급
        CompanyGrade effectiveGrade, // 지금 유효한 등급 (만료/체험 반영, 정책이 실제로 참조하는 값)
        SubscriptionStatus status,
        LocalDate startedAt,
        LocalDate expiresAt,         // null = 무기한(만료 없음)

        // effectiveGrade 가 허용하는 입력 한도. 수정 폼이 입력 개수를 제어하는 근거다.
        //
        // 등급 이름만 내려주면 프론트가 "ENTERPRISE 는 카테고리 몇 개까지인가" 를 스스로 알아야 하고,
        // 그러려면 등급→한도 표를 한 벌 더 들게 된다. 등록 폼(CompanyVerificationResponse)에서 이미
        // 피한 상황이라 수정 폼도 같은 방식으로 막는다 — 한도의 단일 출처는 GradePolicyRegistry 다.
        //
        // grade 가 아니라 effectiveGrade 로 계산한다. 체험이 끝났거나 만료된 구독은 grade 가 그대로여도
        // 정책이 실제로 참조하는 값은 FREE 라, grade 기준으로 한도를 내리면 폼이 통과시킨 값을
        // 저장 시점에 서버가 막는다.
        GradeLimits limits
) {

    // companyName 은 CompanySubscription 이 갖지 않으므로(companyId 만 raw 참조) 호출부가 회사에서 읽어 넘긴다.
    // policy 도 마찬가지로 호출부가 GradePolicyRegistry 에서 읽어 넘긴다 — 이 DTO 가 레지스트리를 알 필요는 없다.
    public static CompanySubscriptionResponse from(CompanySubscription subscription, String companyName,
                                                   GradePolicy policy) {
        return new CompanySubscriptionResponse(
                subscription.getCompanyId(),
                companyName,
                subscription.getGrade(),
                subscription.effectiveGrade(),
                subscription.getStatus(),
                subscription.getStartedAt(),
                subscription.getExpiresAt(),
                GradeLimits.from(policy));
    }
}
