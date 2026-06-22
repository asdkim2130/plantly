package project.plantly.domain.user.enums;

// 등급의 정체성(어떤 등급이 존재하는가) 만 표현한다.
// 등급별 정책 제약(카테고리 상한 등) 은 GradePolicy / GradePolicyRegistry 가 책임진다.
public enum UserGrade {
    BASIC,
    ENTERPRISE,
    ENTERPRISE_TRIAL,
    STANDARD,
    PREMIUM
}