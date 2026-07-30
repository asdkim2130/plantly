package project.plantly.companyTest.countryTest;

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
import project.plantly.domain.company.country.Continent;
import project.plantly.domain.company.country.CountryController;
import project.plantly.domain.company.country.CountryService;
import project.plantly.domain.company.country.dto.CountryPublicResponse;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = CountryController.class)
@ExtendWith(RestDocumentationExtension.class)   // REST Docs: 스니펫 생성 컨텍스트 제공
public class CountryControllerTest {

    // CategoryControllerTest 와 동일하게 WebApplicationContext 로 MockMvc 를 직접 구성한다.
    // 시큐리티 필터는 붙지 않으며, 이 컨트롤러엔 @PreAuthorize 도 없다(공개 조회 전용).
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withResponseDefaults(prettyPrint()))
                .build();
    }

    @MockitoBean
    private CountryService service;

    @Test
    @DisplayName("공개 국가 목록은 인증 없이 200 과 평면 목록을 반환한다")
    public void getPublicList_success () throws Exception {
        // 대륙이 다른 두 건을 샘플로 둔다 — 프론트가 continent 로 group by 해서 2단 UI 를 그린다는
        // 계약이 country-public-list 스니펫에서 보이게 된다.
        CountryPublicResponse korea =
                new CountryPublicResponse(1L, "KR", "대한민국", "South Korea", Continent.ASIA);
        CountryPublicResponse germany =
                new CountryPublicResponse(2L, "DE", "독일", "Germany", Continent.EUROPE);

        given(service.getPublicList()).willReturn(List.of(korea, germany));
        // 인증 주입을 하지 않는다 — 로그인 없이 접근 가능해야 한다.

        mockMvc.perform(get("/api/v1/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].code").value("KR"))
                .andExpect(jsonPath("$.data[0].nameKo").value("대한민국"))
                .andExpect(jsonPath("$.data[0].nameEn").value("South Korea"))
                .andExpect(jsonPath("$.data[0].continent").value("ASIA"))
                // 중첩이 아니라 평면이다 — 대륙 그룹핑은 프론트 몫이다.
                .andExpect(jsonPath("$.data[1].continent").value("EUROPE"))
                .andExpect(jsonPath("$.data[0].countries").doesNotExist())
                // 드롭다운에 쓰이지 않는 식별자는 공개 응답에 없다
                .andExpect(jsonPath("$.data[0].alpha3").doesNotExist())
                .andExpect(jsonPath("$.data[0].numericCode").doesNotExist())
                .andDo(document("country-public-list",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).optional()
                                        .description("응답 메시지 (단순 조회는 생략됨)"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY)
                                        .description("전체 국가 목록 (ISO 3166-1, 250건, 국가명 한글 오름차순). "
                                                + "대륙 → 국가 2단 선택은 continent 로 그룹핑해서 그린다"),
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                        .description("국가 ID. 회사 등록/수정의 수출국(countryIds)에 그대로 실어 보낸다"),
                                fieldWithPath("data[].code").type(JsonFieldType.STRING)
                                        .description("ISO 3166-1 alpha-2 코드. 국기 아이콘/이모지 렌더에 쓸 수 있다"),
                                fieldWithPath("data[].nameKo").type(JsonFieldType.STRING)
                                        .description("국가명 (한글). 목록 정렬 기준이다"),
                                fieldWithPath("data[].nameEn").type(JsonFieldType.STRING)
                                        .description("국가명 (영문). 영문 타이핑 검색용"),
                                fieldWithPath("data[].continent").type(JsonFieldType.STRING)
                                        .description("대륙 구분: ASIA / EUROPE / AFRICA / NORTH_AMERICA / "
                                                + "SOUTH_AMERICA / OCEANIA / ANTARCTICA. 그룹 라벨과 노출 순서는 클라이언트가 정한다"),
                                fieldWithPath("error").type(JsonFieldType.STRING).optional()
                                        .description("에러 메시지 (성공 시 생략됨)")
                        )
                ));
    }

    @Test
    @DisplayName("국가가 없으면 200 과 빈 배열을 반환한다")
    public void getPublicList_empty () throws Exception {
        given(service.getPublicList()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
