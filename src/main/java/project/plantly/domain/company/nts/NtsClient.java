package project.plantly.domain.company.nts;

import project.plantly.domain.company.enums.VerificationOutcome;

import java.time.LocalDate;

/**
 * 국세청 사업자등록정보 조회 게이트웨이.
 *
 * <p>인터페이스로 두는 이유는 두 가지다. (1) 로컬/테스트에서 외부 호출 없이 {@link FakeNtsClient} 로
 * 대체해야 하고, (2) 진위확인(validate)과 상태조회(status) 두 API 를 묶는 조합 규칙을 구현체 안에
 * 가둬 호출부(서비스)가 국세청 응답 스펙을 몰라도 되게 하기 위해서다.
 *
 * <p>결과 타입과 전용 예외는 이 인터페이스 밖에서 쓸 일이 없어 안에 함께 둔다.
 */
public interface NtsClient {

    /**
     * 사업자번호·대표자명·개업일자로 진위확인 후, 이어서 사업자 상태(계속/휴업/폐업)를 조회한다.
     *
     * <p>진위확인만으로는 폐업 사업자를 걸러내지 못한다 — 폐업해도 등록 이력은 남아 있어 valid 가 나온다.
     * 그래서 두 API 를 모두 호출하고, 결과를 하나의 {@link Result} 로 합쳐 돌려준다.
     *
     * @param businessNumber 하이픈 없는 숫자 10자리 (정규화는 호출부 책임)
     * @param ceoName        대표자 성명
     * @param startDate      개업일자 (설립일자가 아니다 — 법인 등기일과 다를 수 있음)
     * @throws UnavailableException 타임아웃·5xx·쿼터 초과 등 국세청 쪽 문제로 판정 자체를 못한 경우.
     *                              사용자 입력 오류(불일치)와 반드시 구분해서 다뤄야 한다.
     */
    Result verify(String businessNumber, String ceoName, LocalDate startDate);

    /**
     * 진위확인 + 상태조회를 합친 판정 결과.
     *
     * <p>outcome 은 국세청이 내려준 다섯 가지({@code VALID/MISMATCH/NOT_REGISTERED/SUSPENDED/CLOSED})
     * 중 하나다 — {@code UNAVAILABLE}/{@code DUPLICATE} 는 이 층이 만들지 않는다.
     *
     * @param rawResponse 국세청 원본 응답(JSON). 감사 로그에 그대로 보관해 분쟁·재검증 근거로 쓴다.
     */
    record Result(VerificationOutcome outcome, String rawResponse) {

        public boolean isValid() {
            return outcome == VerificationOutcome.VALID;
        }
    }

    /**
     * 국세청 API 를 호출하지 못했거나 응답을 해석하지 못한 경우.
     *
     * <p>"사업자 정보가 틀렸다"({@link VerificationOutcome#MISMATCH})와 반드시 구분한다. 이걸 뭉개면
     * 국세청 점검 시간에 사용자에게 "입력하신 정보가 일치하지 않습니다"가 떠서, 멀쩡한 사업자가
     * 자기 정보를 계속 고쳐보는 상황이 생긴다.
     */
    class UnavailableException extends RuntimeException {

        public UnavailableException(String message, Throwable cause) {
            super(message, cause);
        }

        public UnavailableException(String message) {
            super(message);
        }
    }
}
