package project.plantly.companyTest.ntsTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import project.plantly.domain.company.enums.VerificationOutcome;
import project.plantly.domain.company.nts.NtsClient;
import project.plantly.domain.company.nts.NtsRestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

// NtsRestClient 의 실제 HTTP 바인딩 검증. 나머지 테스트는 전부 FakeNtsClient 를 쓰므로,
// "국세청 응답 JSON 을 우리 판정으로 옮기는" 이 층은 여기서만 실물 형태로 확인한다.
// (MockRestServiceServer 로 붙이므로 외부 네트워크는 타지 않는다)
@DisplayName("NtsRestClient: 국세청 응답 → 판정 매핑")
class NtsRestClientTest {

    private static final String BASE_URL = "https://nts.test/v1";
    private static final String B_NO = "1234567890";

    private MockRestServiceServer server;
    private NtsRestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new NtsRestClient(builder.build(), new ObjectMapper(), "test-key");
    }

    // 국세청 진위확인 응답. valid "01" = 일치, "02" = 불일치.
    private String validateResponse(String valid) {
        return """
                {"request_cnt":1,"valid_cnt":1,"data":[{"b_no":"%s","valid":"%s"}]}
                """.formatted(B_NO, valid);
    }

    // 국세청 상태조회 응답. b_stt_cd "01" 계속 / "02" 휴업 / "03" 폐업, 미등록이면 빈 값.
    private String statusResponse(String statusCode) {
        return """
                {"request_cnt":1,"match_cnt":1,"data":[{"b_no":"%s","b_stt_cd":"%s"}]}
                """.formatted(B_NO, statusCode);
    }

    private void expectValidateThenStatus(String valid, String statusCode) {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/validate")))
                .andExpect(method(POST))
                // 국세청은 개업일자를 yyyyMMdd 로 받는다 — LocalDate 를 그대로 직렬화하면 안 된다.
                .andExpect(jsonPath("$.businesses[0].start_dt").value("20200102"))
                .andExpect(jsonPath("$.businesses[0].b_no").value(B_NO))
                .andExpect(jsonPath("$.businesses[0].p_nm").value("홍길동"))
                .andRespond(withSuccess(validateResponse(valid), MediaType.APPLICATION_JSON));

        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/status")))
                .andExpect(method(POST))
                .andRespond(withSuccess(statusResponse(statusCode), MediaType.APPLICATION_JSON));
    }

    private NtsClient.Result verify() {
        return client.verify(B_NO, "홍길동", LocalDate.of(2020, 1, 2));
    }

    @Test
    @DisplayName("진위 일치 + 계속사업자면 VALID")
    void validAndActive() {
        expectValidateThenStatus("01", "01");

        assertThat(verify().outcome()).isEqualTo(VerificationOutcome.VALID);
        server.verify();
    }

    @Test
    @DisplayName("진위는 일치해도 폐업자면 CLOSED — 진위확인만 봤다면 통과했을 케이스")
    void validButClosed() {
        expectValidateThenStatus("01", "03");

        assertThat(verify().outcome()).isEqualTo(VerificationOutcome.CLOSED);
    }

    @Test
    @DisplayName("진위는 일치해도 휴업자면 SUSPENDED")
    void validButSuspended() {
        expectValidateThenStatus("01", "02");

        assertThat(verify().outcome()).isEqualTo(VerificationOutcome.SUSPENDED);
    }

    @Test
    @DisplayName("번호는 있으나 대표자명·개업일자가 다르면 MISMATCH")
    void mismatch() {
        expectValidateThenStatus("02", "01");

        assertThat(verify().outcome()).isEqualTo(VerificationOutcome.MISMATCH);
    }

    @Test
    @DisplayName("상태 코드가 비어 있으면(미등록 번호) MISMATCH 보다 NOT_REGISTERED 를 우선한다")
    void notRegistered() {
        expectValidateThenStatus("02", "");

        assertThat(verify().outcome()).isEqualTo(VerificationOutcome.NOT_REGISTERED);
    }

    @Test
    @DisplayName("두 응답을 합쳐 원본을 보관한다 (분쟁 시 판정 근거 재구성용)")
    void keepsRawResponse() {
        expectValidateThenStatus("01", "01");

        String raw = verify().rawResponse();

        assertThat(raw).contains("validate").contains("status").contains(B_NO);
    }

    @Test
    @DisplayName("국세청이 5xx 를 주면 판정 불가(NtsUnavailableException) — 절대 불일치로 다루지 않는다")
    void serverError_throwsUnavailable() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/validate")))
                .andRespond(withServerError());

        assertThatThrownBy(this::verify).isInstanceOf(NtsClient.UnavailableException.class);
    }

    @Test
    @DisplayName("응답 형식이 예상과 다르면 판정 불가로 처리한다")
    void malformedResponse_throwsUnavailable() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/validate")))
                .andRespond(withSuccess("{\"unexpected\":true}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/status")))
                .andRespond(withSuccess(statusResponse("01"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(this::verify).isInstanceOf(NtsClient.UnavailableException.class);
    }

    @Test
    @DisplayName("알 수 없는 상태 코드는 통과시키지 않고 판정 불가로 둔다")
    void unknownStatusCode_throwsUnavailable() {
        expectValidateThenStatus("01", "99");

        assertThatThrownBy(this::verify).isInstanceOf(NtsClient.UnavailableException.class);
    }
}
