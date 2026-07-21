package project.plantly.domain.company.nts;

import lombok.extern.slf4j.Slf4j;
import project.plantly.domain.company.enums.VerificationOutcome;

import java.time.LocalDate;

/**
 * 외부 호출 없이 판정을 흉내내는 로컬/테스트용 클라이언트. app.nts.fake=true 일 때만 뜬다.
 *
 * <p>기본은 통과이고, 사업자번호 끝 4자리로 실패 시나리오를 재현한다. 로컬에서 아무 번호나 넣어도
 * 등록이 되면서, 필요할 때는 각 실패 분기를 의도적으로 태울 수 있게 하기 위한 규칙이다.
 *
 * <pre>
 *   ...0002 → 진위 불일치
 *   ...0003 → 폐업자
 *   ...0004 → 휴업자
 *   ...0005 → 미등록 사업자번호
 *   ...0009 → 국세청 장애 (UnavailableException)
 *   그 외    → 통과
 * </pre>
 */
@Slf4j
public class FakeNtsClient implements NtsClient {

    @Override
    public Result verify(String businessNumber, String ceoName, LocalDate startDate) {
        log.warn("가짜 국세청 클라이언트로 판정합니다 (b_no={}). 운영 환경이라면 app.nts.fake 설정을 확인하세요.", businessNumber);

        String suffix = businessNumber.length() >= 4
                ? businessNumber.substring(businessNumber.length() - 4)
                : businessNumber;

        VerificationOutcome outcome = switch (suffix) {
            case "0002" -> VerificationOutcome.MISMATCH;
            case "0003" -> VerificationOutcome.CLOSED;
            case "0004" -> VerificationOutcome.SUSPENDED;
            case "0005" -> VerificationOutcome.NOT_REGISTERED;
            case "0009" -> throw new UnavailableException("가짜 클라이언트: 국세청 장애 시나리오");
            default -> VerificationOutcome.VALID;
        };

        return new Result(outcome, "{\"fake\":true,\"outcome\":\"" + outcome + "\"}");
    }
}
