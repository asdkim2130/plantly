package project.plantly.domain.company.enums;

// 회사 구독의 생명주기 상태. 등급(CompanyGrade) 과 직교한다.
// 만료(EXPIRED) 는 별도 상태로 저장하지 않고 CompanySubscription.expiresAt 로 파생한다.
public enum SubscriptionStatus {
    // 정상 구독(유료 또는 무기한 FREE).
    ACTIVE,
    // 체험. 등급 혜택을 받되 expiresAt 만료 시 effectiveGrade 가 FREE 로 강등된다.
    TRIAL,
    // 관리자 등록 회사. 등급 한도 정책에서 면제된다. (구조 검증은 면제하지 않는다)
    ADMIN_EXEMPT
}
