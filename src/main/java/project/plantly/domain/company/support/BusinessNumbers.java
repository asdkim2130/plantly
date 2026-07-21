package project.plantly.domain.company.support;

/**
 * 사업자등록번호 정규화.
 *
 * <p>저장 형태를 하이픈 없는 숫자 10자리로 통일한다. 표기가 섞이면(1234567890 vs 123-45-67890)
 * 활성 부분 유니크 인덱스가 같은 사업자를 다른 값으로 보고 통과시켜, 중복 등록을 막지 못한다.
 *
 * <p>체크섬(검증번호) 계산은 하지 않는다. 진위 판정은 국세청이 하는 일이고, 자체 검산을 넣었다가
 * 규칙이 어긋나면 멀쩡한 사업자를 국세청에 물어보기도 전에 막아버린다.
 */
public final class BusinessNumbers {

    private BusinessNumbers() {
    }

    /**
     * 하이픈·공백을 제거한 숫자 10자리를 돌려준다. 형식이 맞지 않으면 null.
     * (호출부가 null 을 보고 "형식 오류" 에러로 변환한다 — 국세청 호출 전에 걸러 쿼터를 아낀다.)
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9]", "");
        return digits.length() == 10 ? digits : null;
    }
}
