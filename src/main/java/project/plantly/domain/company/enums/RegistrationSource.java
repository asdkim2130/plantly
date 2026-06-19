package project.plantly.domain.company.enums;

// 회사 등록 경로. 누가 어떤 경로로 등록했는지 추적(provenance)하기 위한 값.
// USER  - 유저가 직접 자가등록 (등록 즉시 소유자 = 본인)
// ADMIN - 관리자가 대신 등록 (등록 시점엔 소유자 미연동, 추후 관계자 가입 시 연동)
public enum RegistrationSource {
    USER,
    ADMIN
}
