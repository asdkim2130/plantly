package project.plantly.domain.company.enums;

// 회사 구독 등급(순수 tier). "어떤 등급이 존재하는가"(정체성) 만 표현한다.
// 등급별 정책 제약(카테고리 상한 등) 은 GradePolicy / GradePolicyRegistry 가 책임진다.
// 체험/면제/만료 같은 '상태' 는 등급이 아니라 CompanySubscription 의 status/expiresAt 가 표현한다.
public enum CompanyGrade {
    FREE,
    BASIC,
    STANDARD,
    PREMIUM,
    ENTERPRISE
}
