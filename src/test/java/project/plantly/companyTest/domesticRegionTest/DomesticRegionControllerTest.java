package project.plantly.companyTest.domesticRegionTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import project.plantly.domain.company.domesticRegion.DomesticRegionController;
import project.plantly.domain.company.domesticRegion.DomesticRegionService;
import project.plantly.domain.company.domesticRegion.RegionLevel;
import project.plantly.domain.company.domesticRegion.dto.DomesticRegionAdminResponse;
import project.plantly.domain.company.domesticRegion.dto.DomesticRegionPublicResponse;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;
import project.plantly.global.security.UserPrincipal;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ActiveProfiles("test")
@WebMvcTest(controllers = DomesticRegionController.class)
@ExtendWith(RestDocumentationExtension.class)   // REST Docs: 스니펫 생성 컨텍스트 제공
@Import(DomesticRegionControllerTest.MethodSecurityTestConfig.class)
public class DomesticRegionControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {}

    // REST Docs 적용을 위해 WebApplicationContext 로 MockMvc 를 직접 구성한다.
    // webAppContextSetup 은 시큐리티 필터를 붙이지 않으므로 addFilters=false 와 동일하게 동작하고,
    // @PreAuthorize(메서드 시큐리티)는 그대로 적용된다.
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUpMockMvc(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())   // 요청 본문을 보기 좋게 정렬
                        .withResponseDefaults(prettyPrint())) // 응답 본문을 보기 좋게 정렬
                .build();
    }

    @MockitoBean
    private DomesticRegionService service;

    @Test
    @DisplayName("관리자가 행정구역 트리 조회시 200 OK와 중첩 트리 반환")
    public void getTree_admin_success () throws Exception {
        DomesticRegionAdminResponse suwon = DomesticRegionAdminResponse.builder()
                .name("경기도 수원시")
                .active(true)
                .children(List.of())
                .build();

        DomesticRegionAdminResponse gyeonggi = DomesticRegionAdminResponse.builder()
                .name("경기도")
                .active(true)
                .children(List.of(suwon))
                .build();

        DomesticRegionAdminResponse sejong = DomesticRegionAdminResponse.builder()
                .name("세종특별자치시")
                .active(true)
                .children(List.of())
                .build();

        given(service.getTree()).willReturn(List.of(gyeonggi, sejong));
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/admin/domestic-regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("경기도"))
                .andExpect(jsonPath("$.data[0].active").value(true))
                .andExpect(jsonPath("$.data[0].children[0].name").value("경기도 수원시"))
                .andExpect(jsonPath("$.data[1].name").value("세종특별자치시"))
                .andExpect(jsonPath("$.data[1].children").isEmpty())
                .andDo(document("domestic-region-list",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).optional()
                                        .description("응답 메시지 (단순 조회는 생략됨)"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY)
                                        .description("최상위(시도) 행정구역 목록"),
                                fieldWithPath("data[].name").type(JsonFieldType.STRING)
                                        .description("행정구역 이름"),
                                fieldWithPath("data[].active").type(JsonFieldType.BOOLEAN)
                                        .description("활성화 여부"),
                                // 자식(시군구)은 동일 구조가 재귀되므로 subsection 으로 묶어 문서화
                                subsectionWithPath("data[].children").type(JsonFieldType.ARRAY)
                                        .description("하위 시군구 목록 (동일 구조 재귀, 없으면 빈 배열)"),
                                fieldWithPath("error").type(JsonFieldType.STRING).optional()
                                        .description("에러 메시지 (성공 시 생략됨)")
                        )
                ));
    }

    @Test
    @DisplayName("권한 없는 회원이 행정구역 트리 조회 시도시 403 반환")
    public void getTree_member_forbidden () throws Exception {
        authenticate(1L, UserRole.MEMBER);

        mockMvc.perform(get("/api/v1/admin/domestic-regions"))
                .andExpect(status().isForbidden())
                .andDo(document("domestic-region-list-forbidden",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부 (false)"),
                                fieldWithPath("error").type(JsonFieldType.STRING)
                                        .description("에러 메시지 (접근 권한 없음)")
                        )
                ));

        verify(service, never()).getTree();
    }


    @Test
    @DisplayName("공개 지역 옵션 트리는 비로그인도 200 OK — 전국이 첫 항목이고 도 아래에만 시군구가 중첩된다")
    public void getPublicTree_anonymous_success () throws Exception {
        DomesticRegionPublicResponse nation = DomesticRegionPublicResponse.builder()
                .id(1L).shortName("전국").displayName("전국")
                .level(RegionLevel.NATION).children(List.of()).build();

        DomesticRegionPublicResponse seoul = DomesticRegionPublicResponse.builder()
                .id(2L).shortName("서울").displayName("서울")
                .level(RegionLevel.SIDO).children(List.of()).build();

        DomesticRegionPublicResponse suwon = DomesticRegionPublicResponse.builder()
                .id(4L).shortName("수원").displayName("경기 수원시")
                .level(RegionLevel.SIGUNGU).children(List.of()).build();

        DomesticRegionPublicResponse gyeonggi = DomesticRegionPublicResponse.builder()
                .id(3L).shortName("경기").displayName("경기 전역")
                .level(RegionLevel.SIDO).children(List.of(suwon)).build();

        given(service.getPublicTree()).willReturn(List.of(nation, seoul, gyeonggi));
        // 인증 주입 없음 — permitAll 경로임을 함께 고정한다.

        mockMvc.perform(get("/api/v1/domestic-regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // 전국은 시도의 부모가 아니라 형제 루트이고, code 순 정렬 덕에 항상 맨 앞이다.
                .andExpect(jsonPath("$.data[0].level").value("NATION"))
                .andExpect(jsonPath("$.data[0].shortName").value("전국"))
                .andExpect(jsonPath("$.data[0].children").isEmpty())
                // 자식 없는 광역시는 1차에서 바로 확정된다.
                .andExpect(jsonPath("$.data[1].shortName").value("서울"))
                .andExpect(jsonPath("$.data[1].displayName").value("서울"))
                .andExpect(jsonPath("$.data[1].children").isEmpty())
                // 도는 2차가 열리고, 도 자신의 displayName 이 '전역' 배지가 된다.
                .andExpect(jsonPath("$.data[2].displayName").value("경기 전역"))
                .andExpect(jsonPath("$.data[2].children[0].shortName").value("수원"))
                .andExpect(jsonPath("$.data[2].children[0].displayName").value("경기 수원시"))
                .andDo(document("domestic-region-public-list",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).optional()
                                        .description("응답 메시지 (단순 조회는 생략됨)"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY)
                                        .description("최상위 지역 목록 — 전국 + 시도 17개, code 순(전국이 첫 항목)"),
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                        .description("지역 ID — 등록/수정 요청의 domesticRegionIds 에 그대로 넣는다"),
                                fieldWithPath("data[].shortName").type(JsonFieldType.STRING)
                                        .description("드롭다운 항목용 이름 (부모명 없음, 예: 경기 / 수원)"),
                                fieldWithPath("data[].displayName").type(JsonFieldType.STRING)
                                        .description("확정 후 배지용 이름 (예: 서울 / 경기 전역 / 경기 수원시). 시군구는 시/군 접미사 포함"),
                                fieldWithPath("data[].level").type(JsonFieldType.STRING)
                                        .description("지역 레벨 (NATION/SIDO/SIGUNGU)"),
                                // 자식(시군구)은 동일 구조가 재귀되므로 subsection 으로 묶어 문서화
                                subsectionWithPath("data[].children").type(JsonFieldType.ARRAY)
                                        .description("하위 시군구 목록 (동일 구조 재귀). 비어 있으면 1차에서 바로 확정하고, "
                                                + "비어 있지 않으면 2차 드롭다운을 열되 첫 항목 '전역'에는 부모 자신의 id 를 쓴다"),
                                fieldWithPath("error").type(JsonFieldType.STRING).optional()
                                        .description("에러 메시지 (성공 시 생략됨)")
                        )
                ));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // UserControllerTest와 동일한 인증 주입 헬퍼
    private void authenticate(Long userId, UserRole role) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authOf(userId, role));
        SecurityContextHolder.setContext(context);
    }

    private Authentication authOf(Long userId, UserRole role) {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "userStatus", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "userRole", role);
        UserPrincipal principal = new UserPrincipal(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
