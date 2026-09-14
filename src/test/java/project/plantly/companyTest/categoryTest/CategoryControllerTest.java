package project.plantly.companyTest.categoryTest;

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
import project.plantly.domain.company.category.CategoryController;
import project.plantly.domain.company.category.CategoryService;
import project.plantly.domain.company.category.dto.CategoryPublicResponse;

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
@WebMvcTest(controllers = CategoryController.class)
@ExtendWith(RestDocumentationExtension.class)   // REST Docs: 스니펫 생성 컨텍스트 제공
public class CategoryControllerTest {

    // IndustryControllerTest 와 동일하게 WebApplicationContext 로 MockMvc 를 직접 구성한다.
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
    private CategoryService service;

    @Test
    @DisplayName("공개 카테고리 트리는 인증 없이 200 과 중첩 목록을 반환한다")
    public void getPublicTree_success () throws Exception {
        CategoryPublicResponse child =
                new CategoryPublicResponse(2L, "CNC 선반", "mach-cnc", 2, null, List.of());
        CategoryPublicResponse root =
                new CategoryPublicResponse(1L, "기계", "mach", 1, "icon-mach", List.of(child));

        given(service.getPublicTree()).willReturn(List.of(root));
        // 인증 주입을 하지 않는다 — 로그인 없이 접근 가능해야 한다.

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].categoryName").value("기계"))
                .andExpect(jsonPath("$.data[0].slug").value("mach"))
                .andExpect(jsonPath("$.data[0].depth").value(1))
                .andExpect(jsonPath("$.data[0].iconUrl").value("icon-mach"))
                // 중첩 자식이 그대로 내려간다 (대→중→소 3단 선택의 소스)
                .andExpect(jsonPath("$.data[0].children[0].id").value(2))
                .andExpect(jsonPath("$.data[0].children[0].depth").value(2))
                .andExpect(jsonPath("$.data[0].children[0].children").isEmpty())
                // 운영 필드는 공개 응답에 없다
                .andExpect(jsonPath("$.data[0].displayOrder").doesNotExist())
                .andExpect(jsonPath("$.data[0].active").doesNotExist())
                .andExpect(jsonPath("$.data[0].description").doesNotExist())
                .andDo(document("category-public-tree",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).optional()
                                        .description("응답 메시지 (단순 조회는 생략됨)"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY)
                                        .description("활성 대분류 목록 (displayOrder 오름차순). 비활성 노드는 하위까지 함께 제외된다"),
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                        .description("카테고리 ID. 검색 필터(categoryIds)·회사 등록(categoryIds)에 그대로 실어 보낸다. "
                                                + "검색은 어느 depth 든 조상 closure 로 매칭한다"),
                                fieldWithPath("data[].categoryName").type(JsonFieldType.STRING)
                                        .description("카테고리 이름"),
                                fieldWithPath("data[].slug").type(JsonFieldType.STRING)
                                        .description("카테고리 슬러그"),
                                fieldWithPath("data[].depth").type(JsonFieldType.NUMBER)
                                        .description("분류 단계: 1(대분류) / 2(중분류) / 3(소분류)"),
                                fieldWithPath("data[].iconUrl").type(JsonFieldType.STRING).optional()
                                        .description("아이콘 URL (미등록 시 null)"),
                                fieldWithPath("data[].children").type(JsonFieldType.ARRAY)
                                        .description("하위 카테고리 목록 (잎 노드는 빈 배열). 구조는 상위와 동일하게 재귀된다"),
                                fieldWithPath("data[].children[].id").type(JsonFieldType.NUMBER)
                                        .description("하위 카테고리 ID"),
                                fieldWithPath("data[].children[].categoryName").type(JsonFieldType.STRING)
                                        .description("하위 카테고리 이름"),
                                fieldWithPath("data[].children[].slug").type(JsonFieldType.STRING)
                                        .description("하위 카테고리 슬러그"),
                                fieldWithPath("data[].children[].depth").type(JsonFieldType.NUMBER)
                                        .description("분류 단계"),
                                fieldWithPath("data[].children[].iconUrl").type(JsonFieldType.STRING).optional()
                                        .description("아이콘 URL (미등록 시 null)"),
                                fieldWithPath("data[].children[].children").type(JsonFieldType.ARRAY)
                                        .description("하위 카테고리 목록 (소분류는 빈 배열)"),
                                fieldWithPath("code").type(JsonFieldType.STRING).optional()
                                        .description("에러 코드(ErrorCode 상수명). 클라이언트는 문구가 아니라 이 값으로 분기한다. 성공 응답에는 없다"),
                                fieldWithPath("error").type(JsonFieldType.STRING).optional()
                                        .description("에러 메시지 (성공 시 생략됨)")
                        )
                ));
    }

    @Test
    @DisplayName("카테고리가 없으면 200 과 빈 배열을 반환한다")
    public void getPublicTree_empty () throws Exception {
        given(service.getPublicTree()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
