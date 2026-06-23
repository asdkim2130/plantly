package project.plantly.domain.company.policy;

import project.plantly.domain.user.enums.UserGrade;

// 회사 등록 정책 실행에 필요한 컨텍스트.
// grade == null 이면 관리자 선등록(소유자 미연동) 을 의미한다.
public record CompanyRegistrationContext(UserGrade grade) {

    // 관리자 선등록 시 캡 정책이 기준 삼을 절대 상한 등급 = 최상위 등급.
    // "관리자는 무조건 최상위 옵션으로 등록한다" 는 규칙의 단일 출처.
    // 캡 기준을 바꾸려면 여기 한 곳만 수정한다.
    public static final UserGrade ADMIN_CAP_GRADE = UserGrade.ENTERPRISE_TRIAL;

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

    // 상한(cap) 정책이 기준 삼을 등급. 유저는 본인 등급, 관리자는 절대 상한(최상위) 등급을 따른다.
    // CategoryLimitPolicy / DetailImageLimitPolicy 처럼 "등급별 상한" 을 강제하는 정책이 공통으로 사용한다.
    public UserGrade gradeForCap() {
        return isAdminRegistration() ? ADMIN_CAP_GRADE : grade;
    }
}