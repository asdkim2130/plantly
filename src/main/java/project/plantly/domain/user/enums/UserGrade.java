package project.plantly.domain.user.enums;

// 등급의 정체성(어떤 등급이 존재하는가) 만 표현한다.
// 등급별 정책 제약(카테고리 상한 등) 은 GradePolicy / GradePolicyRegistry 가 책임진다.
public enum UserGrade {
    FREE,
    BASIC,
    ENTERPRISE,
    ENTERPRISE_TRIAL,
    STANDARD,
    PREMIUM,
    // 관리자 등록 회사에 부여되는 등급. 어떤 유저도 보유하지 않으며, 등급 한도 정책에서 면제된다.
    // (관리자는 최소 자료로만 등록하므로 제약 불필요. 구조 검증은 면제하지 않는다.)
    ADMIN_REGISTER
}