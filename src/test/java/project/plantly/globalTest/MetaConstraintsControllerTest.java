package project.plantly.globalTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import project.plantly.global.meta.ConstraintForm;
import project.plantly.global.meta.FormConstraintsReader;
import project.plantly.global.meta.MetaConstraintsController;
import project.plantly.global.meta.dto.ConstraintRule;
import project.plantly.global.meta.dto.FieldConstraints;
import project.plantly.global.meta.dto.FormConstraintsResponse;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 제약 조회 엔드포인트의 계약.
 *
 * <p>도출 규칙 자체는 {@link FormConstraintsReaderTest} 가 실제 DTO 로 잠근다. 여기서는 그 결과를
 * 어떤 봉투에 담아 내보내는지와 없는 폼 이름의 처리만 본다 — 그래서 reader 를 대역으로 둔다.
 * 응답 예시가 짧아야 스니펫도 읽힌다.
 */
@ActiveProfiles("test")
@WebMvcTest(controllers = MetaConstraintsController.class)
@ExtendWith(RestDocumentationExtension.class)
@DisplayName("폼 제약 조회 API")
class MetaConstraintsControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private FormConstraintsReader reader;

    @BeforeEach
    void setUpMockMvc(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withResponseDefaults(prettyPrint()))
                .build();
    }

    @Test
    @DisplayName("폼 이름으로 조회하면 필드별 입력 규칙을 폼 순서로 반환한다")
    void getFormConstraints_success() throws Exception {
        // 두 갈래를 한 응답에 담는다 - 단순 입력칸(companyName)과 원소 규칙이 따로 있는 컬렉션(tagNames).
        FieldConstraints companyName = new FieldConstraints("companyName",
                List.of(ConstraintRule.of("required", "기업명은 필수입니다."),
                        ConstraintRule.of("maxLength", 100, "기업명은 100자를 넘을 수 없습니다.")),
                List.of(), null);

        FieldConstraints tagNames = new FieldConstraints("tagNames",
                List.of(ConstraintRule.of("maxItems", 10, "태그는 10개를 넘을 수 없습니다.")),
                List.of(),
                new FieldConstraints(null,
                        List.of(ConstraintRule.of("maxLength", 20, "태그는 20자를 넘을 수 없습니다.")),
                        List.of(), null));

        given(reader.read(ConstraintForm.COMPANY_CREATE))
                .willReturn(new FormConstraintsResponse("company-create", List.of(),
                        List.of(companyName, tagNames)));

        mockMvc.perform(get("/api/v1/meta/constraints/{form}", "company-create"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.form").value("company-create"))
                .andExpect(jsonPath("$.data.fields[0].field").value("companyName"))
                .andExpect(jsonPath("$.data.fields[0].rules[0].type").value("required"))
                // 값이 필요 없는 규칙은 키가 빠진다.
                .andExpect(jsonPath("$.data.fields[0].rules[0].value").doesNotExist())
                .andExpect(jsonPath("$.data.fields[0].rules[1].value").value(100))
                // 개수는 필드에, 길이는 원소에 - 태그 목록이 아니라 태그 한 칸의 제약이다.
                .andExpect(jsonPath("$.data.fields[1].rules[0].type").value("maxItems"))
                .andExpect(jsonPath("$.data.fields[1].items.rules[0].value").value(20))
                // 원소 서술자에는 이름이 없다.
                .andExpect(jsonPath("$.data.fields[1].items.field").doesNotExist())
                // 규칙도 하위 필드도 없는 자리는 빈 배열이 아니라 키 자체가 빠진다.
                .andExpect(jsonPath("$.data.rules").doesNotExist())
                .andExpect(jsonPath("$.data.fields[0].fields").doesNotExist())
                .andExpect(jsonPath("$.data.fields[0].items").doesNotExist())
                .andDo(document("meta-form-constraints",
                        pathParameters(
                                parameterWithName("form").description(
                                        "폼 이름: company-create(관리자 등록) / my-company-create(자가등록) / "
                                                + "company-update(수정). 목록에 없는 이름은 404")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).optional()
                                        .description("응답 메시지 (단순 조회는 생략됨)"),
                                fieldWithPath("data.form").type(JsonFieldType.STRING)
                                        .description("요청한 폼 이름"),
                                fieldWithPath("data.rules").type(JsonFieldType.ARRAY).optional()
                                        .description("필드에 붙지 않는 폼 전체 규칙. 없으면 생략됨"),
                                fieldWithPath("data.fields").type(JsonFieldType.ARRAY)
                                        .description("입력칸별 규칙. 화면의 폼 순서(요청 DTO 선언 순서)로 정렬돼 있다. "
                                                + "내려줄 규칙이 없는 칸(enum, 형식 제약만 걸린 칸)은 목록에 없다"),
                                fieldWithPath("data.fields[].field").type(JsonFieldType.STRING)
                                        .description("요청 본문에서의 필드 이름. 검증 실패 응답의 errors[].field 와 같은 어휘다"),
                                fieldWithPath("data.fields[].rules").type(JsonFieldType.ARRAY)
                                        .description("이 칸에 걸린 규칙 목록. 비어 있음 → 개수 → 길이 순"),
                                fieldWithPath("data.fields[].rules[].type").type(JsonFieldType.STRING)
                                        .description("규칙 종류: required / minLength / maxLength / minItems / maxItems / "
                                                + "min / max. 형식 규칙(정규식·이메일·날짜)은 여기 실리지 않는다 — "
                                                + "우리가 정한 값만 내려간다"),
                                fieldWithPath("data.fields[].rules[].value").type(JsonFieldType.NUMBER).optional()
                                        .description("규칙의 값(길이·개수). 값이 필요 없는 규칙(required)에서는 생략됨"),
                                fieldWithPath("data.fields[].rules[].message").type(JsonFieldType.STRING)
                                        .description("규칙을 어겼을 때 띄울 문구. 서버가 400 으로 되돌려줄 때와 같은 문구다"),
                                fieldWithPath("data.fields[].fields").type(JsonFieldType.ARRAY).optional()
                                        .description("중첩 객체의 하위 입력칸. 없으면 생략됨"),
                                fieldWithPath("data.fields[].items").type(JsonFieldType.OBJECT).optional()
                                        .description("컬렉션 원소의 제약. 필드의 규칙이 '몇 개까지'라면 여기는 '원소 하나가 어때야 하는가'다. "
                                                + "원소가 객체면 items.fields 에 하위 입력칸이 담긴다. 없으면 생략됨"),
                                fieldWithPath("data.fields[].items.rules").type(JsonFieldType.ARRAY)
                                        .description("원소에 걸린 규칙"),
                                fieldWithPath("data.fields[].items.rules[].type").type(JsonFieldType.STRING)
                                        .description("규칙 종류"),
                                fieldWithPath("data.fields[].items.rules[].value").type(JsonFieldType.NUMBER).optional()
                                        .description("규칙의 값(길이·개수)"),
                                fieldWithPath("data.fields[].items.rules[].message").type(JsonFieldType.STRING)
                                        .description("규칙을 어겼을 때 띄울 문구"),
                                fieldWithPath("error").type(JsonFieldType.STRING).optional()
                                        .description("에러 메시지 (성공 시 생략됨)")
                        )
                ));
    }

    // 임의의 클래스 이름을 받아 리플렉션으로 열어보는 창구가 되지 않도록 화이트리스트만 연다.
    // 잘못된 입력값(400)이 아니라 존재하지 않는 자원이므로 404 다.
    @Test
    @DisplayName("등록되지 않은 폼 이름은 404 를 반환한다")
    void getFormConstraints_unknownForm() throws Exception {
        mockMvc.perform(get("/api/v1/meta/constraints/{form}", "company-secret"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("요청한 경로를 찾을 수 없습니다."))
                // 검증 실패가 아니므로 항목별 오류 배열이 실리지 않는다.
                .andExpect(jsonPath("$.errors").doesNotExist());
    }
}
