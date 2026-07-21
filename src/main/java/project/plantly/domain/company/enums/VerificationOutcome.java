package project.plantly.domain.company.enums;

/**
 * 인증 시도 1건의 결말. 감사 로그에 저장되고, 일일 재시도 제한 집계의 기준이 된다.
 *
 * <p>앞의 다섯은 국세청이 내려준 판정이고, 뒤의 둘은 우리 쪽 사유다 —
 * 국세청에 물어보지도 못한 {@link #UNAVAILABLE}, 판정은 통과했지만 이미 등록된 번호였던 {@link #DUPLICATE}.
 * (그래서 {@code NtsClient} 는 앞의 다섯만 반환한다. 뒤의 둘은 서비스가 붙인다.)
 */
public enum VerificationOutcome {

    /** 진위 일치 + 계속사업자. 인증 성공. */
    VALID,

    /** 사업자번호·대표자명·개업일자 조합이 국세청 등록 정보와 불일치. */
    MISMATCH,

    /** 국세청에 등록되지 않은 사업자번호. */
    NOT_REGISTERED,

    /** 휴업자. 진위는 맞지만 현재 영업 상태가 아니다. */
    SUSPENDED,

    /** 폐업자. 진위는 맞지만 사업자등록이 종료됐다. */
    CLOSED,

    /** 국세청 API 장애·타임아웃으로 판정 자체를 못한 경우. */
    UNAVAILABLE,

    /** 국세청 판정은 통과했으나 이미 활성 회사가 쓰고 있는 사업자번호인 경우. */
    DUPLICATE;

    public boolean countsTowardDailyLimit() {
        // 국세청이 죽어서 실패한 시도까지 세면, 장애 시간에 재시도한 사용자가 하루치 기회를 잃는다.
        return this != UNAVAILABLE;
    }
}
