package project.plantly.domain.company.nts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import project.plantly.domain.company.enums.VerificationOutcome;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 공공데이터포털 국세청 사업자등록정보 API 실제 호출 구현.
 *
 * <p>진위확인(POST /validate)과 상태조회(POST /status)를 순서대로 호출한다. 두 번 부르는 이유는
 * 진위확인이 "세 값이 서로 맞는가"만 답하고 폐업 여부는 알려주지 않기 때문이다. 폐업 사업자도
 * 등록 이력이 남아 있어 valid 가 나온다.
 *
 * <p>진위확인이 실패했을 때도 상태조회를 부르는데, 이건 "등록되지 않은 번호"와 "번호는 있지만
 * 대표자명/개업일자가 다름"을 구분해 안내 문구를 정확히 주기 위해서다. 호출 횟수는 서비스 레이어의
 * 일일 재시도 제한(userId 기준)으로 통제한다.
 */
@Slf4j
public class NtsRestClient implements NtsClient {

    private static final DateTimeFormatter START_DT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 진위확인 응답의 data[].valid: "01" = 일치, "02" = 불일치
    private static final String VALID_MATCH = "01";

    // 상태조회 응답의 data[].b_stt_cd: "01" 계속사업자 / "02" 휴업자 / "03" 폐업자.
    // 미등록 번호는 b_stt_cd 가 빈 값으로 내려온다(tax_type 에 안내 문구).
    private static final String STATUS_ACTIVE = "01";
    private static final String STATUS_SUSPENDED = "02";
    private static final String STATUS_CLOSED = "03";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String serviceKey;

    public NtsRestClient(RestClient restClient, ObjectMapper objectMapper, String serviceKey) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.serviceKey = serviceKey;
    }

    @Override
    public Result verify(String businessNumber, String ceoName, LocalDate startDate) {
        String validateBody = callValidate(businessNumber, ceoName, startDate);
        String statusBody = callStatus(businessNumber);

        boolean matched = readValid(validateBody);
        String statusCode = readStatusCode(statusBody);

        return new Result(decide(matched, statusCode), mergeRaw(validateBody, statusBody));
    }

    // 진위확인/상태조회 결과를 하나의 판정으로 합친다.
    // 미등록 판정을 진위 불일치보다 우선하는 이유: 번호 자체가 없으면 대표자명·개업일자를 고쳐봐야 소용없고,
    // 사용자에게도 "등록되지 않은 번호"라고 알려주는 편이 훨씬 명확하다.
    private VerificationOutcome decide(boolean matched, String statusCode) {
        if (statusCode == null || statusCode.isBlank()) {
            return VerificationOutcome.NOT_REGISTERED;
        }
        if (!matched) {
            return VerificationOutcome.MISMATCH;
        }
        return switch (statusCode) {
            case STATUS_ACTIVE -> VerificationOutcome.VALID;
            case STATUS_SUSPENDED -> VerificationOutcome.SUSPENDED;
            case STATUS_CLOSED -> VerificationOutcome.CLOSED;
            // 국세청이 새 상태 코드를 추가했거나 응답이 예상 밖인 경우. 통과시키는 것보다 판정 불가로 두는 편이 안전하다.
            default -> throw new UnavailableException("알 수 없는 사업자 상태 코드: " + statusCode);
        };
    }

    private String callValidate(String businessNumber, String ceoName, LocalDate startDate) {
        Map<String, Object> body = Map.of("businesses", List.of(Map.of(
                "b_no", businessNumber,
                "start_dt", startDate.format(START_DT),
                "p_nm", ceoName
        )));
        return post("/validate", body);
    }

    private String callStatus(String businessNumber) {
        return post("/status", Map.of("b_no", List.of(businessNumber)));
    }

    private String post(String path, Object body) {
        try {
            // serviceKey 는 공공데이터포털의 '디코딩된' 키를 그대로 넘긴다. queryParam 이 인코딩을 책임지므로
            // 인코딩된 키를 넣으면 이중 인코딩되어 SERVICE_KEY_IS_NOT_REGISTERED_ERROR 가 난다.
            return restClient.post()
                    .uri(uriBuilder -> uriBuilder.path(path).queryParam("serviceKey", serviceKey).build())
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            // 타임아웃·연결 실패·4xx/5xx 를 모두 여기서 "판정 불가"로 승격시킨다.
            // 사용자 입력 문제가 아니므로 절대 불일치로 다루지 않는다.
            log.warn("국세청 API 호출 실패 (path={})", path, e);
            throw new UnavailableException("국세청 API 호출에 실패했습니다: " + path, e);
        }
    }

    private boolean readValid(String validateBody) {
        return VALID_MATCH.equals(firstData(validateBody, "진위확인").path("valid").asText());
    }

    private String readStatusCode(String statusBody) {
        return firstData(statusBody, "상태조회").path("b_stt_cd").asText();
    }

    private JsonNode firstData(String body, String label) {
        try {
            JsonNode data = objectMapper.readTree(body).path("data");
            if (!data.isArray() || data.isEmpty()) {
                throw new UnavailableException(label + " 응답에 data 가 없습니다.");
            }
            return data.get(0);
        } catch (UnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new UnavailableException(label + " 응답을 해석하지 못했습니다.", e);
        }
    }

    // 두 응답을 한 덩어리로 묶어 감사 로그에 남긴다. 나중에 분쟁이 생기면 어떤 근거로 통과/차단했는지 재구성해야 한다.
    private String mergeRaw(String validateBody, String statusBody) {
        try {
            ObjectNode merged = objectMapper.createObjectNode();
            merged.set("validate", objectMapper.readTree(validateBody));
            merged.set("status", objectMapper.readTree(statusBody));
            return objectMapper.writeValueAsString(merged);
        } catch (Exception e) {
            // 감사 로그 보관 실패가 인증 자체를 막을 이유는 없다. 원문을 그대로 이어붙여 남긴다.
            return "{\"validate\":" + validateBody + ",\"status\":" + statusBody + "}";
        }
    }
}
