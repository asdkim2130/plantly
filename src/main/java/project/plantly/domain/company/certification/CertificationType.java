package project.plantly.domain.company.certification;

// 인증 그룹핑. 시드(certification.sql)의 type 값과 1:1로 매핑된다.
public enum CertificationType {
    MANAGEMENT_SYSTEM,   // 경영시스템 (ISO 9001/14001/45001 등)
    INDUSTRY_SPECIFIC,   // 산업특화 (IATF 16949, SQ 등)
    MARKET_ACCESS,        // 시장진입 (KC, CE, UL 등)
    ETC                   // 자유입력
}
