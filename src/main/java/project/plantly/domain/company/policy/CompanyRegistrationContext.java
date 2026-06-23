package project.plantly.domain.company.policy;

import project.plantly.domain.user.enums.UserGrade;

// 회사 등록 정책 실행에 필요한 컨텍스트.
// 등록 경로에 따라 회사의 초기 등급이 결정된다: 유저 자가등록 = FREE, 관리자 등록 = ADMIN_REGISTER(등급 한도 면제).
public record CompanyRegistrationContext(UserGrade grade) {

    // 유저 자가등록: 소유자 = 본인. 정책은 본인 등급을 기준으로 적용된다.
    public static CompanyRegistrationContext ofUser(UserGrade grade) {
        return new CompanyRegistrationContext(grade);
    }

    // 관리자 등록: 정책 면제 등급(ADMIN_REGISTER). 등급 한도 정책은 이 등급을 스킵한다.
    // (관리자는 최소 자료로만 등록하므로 제약 불필요)
    public static CompanyRegistrationContext ofAdmin() {
        return new CompanyRegistrationContext(UserGrade.ADMIN_REGISTER);
    }

    // 관리자 등록 여부. 등급 한도 정책이 이 컨텍스트를 면제 대상으로 판단하는 데 쓴다.
    // (구조 검증 정책(GalleryImageTypePolicy 등)은 면제하지 않고 전원 적용한다)
    public boolean isAdminRegistration() {
        return grade == UserGrade.ADMIN_REGISTER;
    }
}