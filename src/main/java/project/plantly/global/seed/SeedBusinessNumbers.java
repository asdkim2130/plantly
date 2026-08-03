package project.plantly.global.seed;

/**
 * 시드가 발급하는 사업자등록번호 체계.
 *
 * <p>{@code BusinessNumbers.normalize} 는 숫자 10자리만 요구하고 체크섬은 보지 않으므로(진위 판정은
 * 국세청의 일) 순번으로 충분하다. 다만 {@code FakeNtsClient} 가 <b>끝 4자리</b>로 실패 시나리오를
 * 분기하기 때문에(0002 불일치 / 0003 폐업 / 0004 휴업 / 0005 미등록 / 0009 장애), 정상 회사의 번호는
 * 그 구간을 피해 1000번대 이상에서만 발급한다. 실패 케이스는 반대로 그 번호를 의도적으로 쓴다.
 */
public final class SeedBusinessNumbers {

    private static final String PREFIX = "700000";

    /**
     * 시드 잔여물 판정용 마커(C01 의 번호). 완료 판정은 {@link SeedState} 의 표식이 하고, 이 번호는
     * "완료되지 않았는데 데이터가 남아 있다"를 가려내는 데만 쓴다 — C01 은 가장 먼저 만들어지므로
     * 이것만으로 완료를 판정하면 뒤쪽에서 깨진 시드를 완료로 오인한다.
     */
    public static final String MARKER = caseNumber(1);

    // ===== FakeNtsClient 실패 분기용 (D07 감사 로그) =====
    public static final String NTS_MISMATCH = PREFIX + "0002";
    public static final String NTS_CLOSED = PREFIX + "0003";
    public static final String NTS_NOT_REGISTERED = PREFIX + "0005";

    private SeedBusinessNumbers() {
    }

    /** 계약 케이스 회사 C01~C24 → 7000001001 ~ 7000001024 */
    public static String caseNumber(int sequence) {
        return PREFIX + String.format("1%03d", sequence);
    }

    /** 정족수 패딩 회사 → 7000002001 ~ */
    public static String paddingNumber(int sequence) {
        return PREFIX + String.format("2%03d", sequence);
    }

    /** 인증·초안 케이스 D01~ → 7000003001 ~ */
    public static String draftNumber(int sequence) {
        return PREFIX + String.format("3%03d", sequence);
    }
}
