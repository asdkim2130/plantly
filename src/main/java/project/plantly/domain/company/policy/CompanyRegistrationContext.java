package project.plantly.domain.company.policy;

import project.plantly.domain.user.enums.UserGrade;

// 회사 등록 정책 실행에 필요한 컨텍스트.
// grade == null 이면 관리자 선등록(소유자 미연동) 을 의미한다.
public record CompanyRegistrationContext(UserGrade grade) {

    // 유저 자가등록: 소유자 = 본인. 정책은 본인 등급을 기준으로 적용된다.
    public static CompanyRegistrationContext ofUser(UserGrade grade) {
        return new CompanyRegistrationContext(grade);
    }

    // 관리자 등록: 소유자(=등급) 가 없는 상태로 시작한다. 등급에 의존하는 정책은 각자 기본 처리를 결정한다.
    public static CompanyRegistrationContext ofAdmin() {
        return new CompanyRegistrationContext(null);
    }

    public boolean isAdminRegistration() {
        return grade == null;
    }
}