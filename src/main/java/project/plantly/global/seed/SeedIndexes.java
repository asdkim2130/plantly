package project.plantly.global.seed;

/**
 * 어휘 조합에 쓰는 순번 배분.
 *
 * <p>{@link SeedVocabulary#companyName} 은 어근 25 × 업종어 20 = 500 조합을 순번으로 펼친다. 순번을
 * 1,2,3… 으로 주면 앞자리만 바뀌어 "대성정밀 / 우진정밀 / 한신정밀" 처럼 뒷말이 전부 같아진다.
 * 서로소인 stride 로 건너뛰면 500 안에서 겹치지 않으면서 조합이 골고루 흩어진다.
 */
public final class SeedIndexes {

    private static final int STRIDE = 17;
    private static final int CASE_COUNT = 24;
    private static final int PADDING_COUNT = 24;

    private SeedIndexes() {
    }

    /** 계약 케이스 C01~C24 */
    public static int forCase(int caseNo) {
        return caseNo * STRIDE;
    }

    /** 정족수 패딩 P01~P24 */
    public static int forPadding(int sequence) {
        return (CASE_COUNT + sequence) * STRIDE;
    }

    /** 인증·초안 케이스 D01~ */
    public static int forDraft(int sequence) {
        return (CASE_COUNT + PADDING_COUNT + sequence) * STRIDE;
    }
}
