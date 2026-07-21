package project.plantly.domain.company.enums;

/**
 * 사업자 인증 레코드의 생애주기.
 *
 * <p>불리언 대신 상태로 두는 이유는 {@link #REVOKED} 때문이다. 관리자가 사칭 신고를 받고 인증을
 * 회수했을 때 불리언이면 미인증으로 되돌아가 곧바로 재인증이 가능해진다. 상태로 두면 회수 사실과
 * 사유가 남고, 같은 번호의 재시도를 막을 근거가 된다.
 */
public enum VerificationStatus {

    /** 국세청 진위확인·상태조회를 통과했고 아직 회사 등록에 쓰이지 않은 상태. 만료 시각(expiresAt)이 있다. */
    VERIFIED,

    /** 회사 등록에 사용됨. 이 시점에 companyId 가 채워지고 재사용이 불가능해진다. */
    CONSUMED,

    /** 만료 시각을 넘겨 사용할 수 없게 된 상태. */
    EXPIRED,

    /** 관리자가 인증을 회수한 상태. */
    REVOKED
}
