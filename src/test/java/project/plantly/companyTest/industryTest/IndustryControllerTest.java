package project.plantly.companyTest.industryTest;

import project.plantly.domain.company.industry.IndustryErrorCode;
import project.plantly.global.exception.BusinessException;
import tools.jackson.databind.ObjectMapper;
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
import project.plantly.domain.company.industry.IndustryController;
import project.plantly.domain.company.industry.IndustryService;
import project.plantly.domain.company.industry.dto.IndustryAdminResponse;
import project.plantly.domain.company.industry.dto.IndustryCreateRequest;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;
import project.plantly.global.security.UserPrincipal;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ActiveProfiles("test")
@WebMvcTest(controllers = IndustryController.class)
@ExtendWith(RestDocumentationExtension.class)   // REST Docs: 스니펫 생성 컨텍스트 제공
@Import(IndustryControllerTest.MethodSecurityTestConfig.class)
public class IndustryControllerTest {

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
    private IndustryService service;

    @Test
    @DisplayName("관리자가 산업군 생성시 200 OK와 id 반환")
    public void create_admin_success () throws Exception {
        IndustryCreateRequest request = new IndustryCreateRequest("a", "a", "url", "상세설명", 0);

        given(service.createIndustry(any(IndustryCreateRequest.class))).willReturn(1L);
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(post("/api/v1/admin/industries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("산업군 생성이 완료되었습니다."))
                .andExpect(jsonPath("$.data").value(1L))
                .andDo(document("industry-create",
                        requestFields(
                                fieldWithPath("industryName").type(JsonFieldType.STRING)
                                        .description("산업군 이름"),
                                fieldWithPath("industryCode").type(JsonFieldType.STRING)
                                        .description("산업군 코드 (영문/숫자/하이픈)"),
                                fieldWithPath("iconUrl").type(JsonFieldType.STRING).optional()
                                        .description("아이콘 URL"),
                                fieldWithPath("description").type(JsonFieldType.STRING).optional()
                                        .description("산업군 상세 설명"),
                                fieldWithPath("displayOrder").type(JsonFieldType.NUMBER).optional()
                                        .description("노출 순서 (0 이상, 미입력 시 마지막 순번 + 1 자동 부여)")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING)
                                        .description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NUMBER)
                                        .description("생성된 산업군 ID"),
                                fieldWithPath("error").type(JsonFieldType.STRING).optional()
                                        .description("에러 메시지 (성공 시 응답에서 생략됨)")
                        )
                ));
    }

    @Test
    @DisplayName("권한 없는 회원이 산업군 생성 시도시 403 반환, 서비스 호출되지 않음")
    public void create_member_forbidden () throws Exception {
        IndustryCreateRequest request = new IndustryCreateRequest("a", "a", null, null, null);
        authenticate(1L, UserRole.MEMBER);

        mockMvc.perform(post("/api/v1/admin/industries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andDo(document("industry-create-forbidden",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부 (false)"),
                                fieldWithPath("error").type(JsonFieldType.STRING)
                                        .description("에러 메시지 (접근 권한 없음)")
                        )
                ));

        verify(service, never()).createIndustry(any());
    }

    @Test
    @DisplayName("industryCode에 한글이 들어가면 400에러와 검증 에러 반환")
    public void create_invalidCode_validationFail() throws Exception {
        IndustryCreateRequest request = new IndustryCreateRequest("한글", "한글", null, null, null);
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(post("/api/v1/admin/industries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists())
                .andDo(document("industry-create-validation-error",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부 (false)"),
                                fieldWithPath("error").type(JsonFieldType.STRING)
                                        .description("검증 실패 메시지 (첫 번째 위반 항목)")
                        )
                ));
    }

    @Test
    @DisplayName("이름이 중복이면 409를 반환")
    public void create_duplicateName_conflict () throws Exception {
        IndustryCreateRequest request = new IndustryCreateRequest("a", "a", null, null, null);
        willThrow(new BusinessException(IndustryErrorCode.DUPLICATE_INDUSTRY_NAME))
                .given(service).createIndustry(any(IndustryCreateRequest.class));

        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(post("/api/v1/admin/industries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("산업군 이름은 중복 불가합니다."))
                .andDo(document("industry-create-duplicate-name",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부 (false)"),
                                fieldWithPath("error").type(JsonFieldType.STRING)
                                        .description("에러 메시지 (산업군 이름 중복)")
                        )
                ));
    }

    @Test
    @DisplayName("코드가 중복이면 409를 반환")
    public void create_duplicateCode_conflict () throws Exception {
        IndustryCreateRequest request = new IndustryCreateRequest("a", "a", null, null, null);
        willThrow(new BusinessException(IndustryErrorCode.DUPLICATE_INDUSTRY_CODE))
                .given(service).createIndustry(any(IndustryCreateRequest.class));

        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(post("/api/v1/admin/industries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("산업군 코드는 중복 불가합니다."))
                .andDo(document("industry-create-duplicate-code",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부 (false)"),
                                fieldWithPath("error").type(JsonFieldType.STRING)
                                        .description("에러 메시지 (산업군 코드 중복)")
                        )
                ));
    }


    @Test
    @DisplayName("관리자가 산업군 전체 조회시 200 OK와 목록 반환")
    public void getAll_admin_success () throws Exception {
        IndustryAdminResponse response = IndustryAdminResponse.builder()
                .id(1L)
                .industryName("농업")
                .industryCode("agri")
                .iconUrl("icon-agri")
                .description("농업 설명")
                .displayOrder(0)
                .active(true)
                .build();

        given(service.getAll()).willReturn(List.of(response));
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/admin/industries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].industryCode").value("agri"))
                .andExpect(jsonPath("$.data[0].iconUrl").value("icon-agri"))
                .andDo(document("industry-list",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).optional()
                                        .description("응답 메시지 (단순 조회는 생략됨)"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY)
                                        .description("산업군 목록 (displayOrder 오름차순)"),
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                        .description("산업군 ID"),
                                fieldWithPath("data[].industryName").type(JsonFieldType.STRING)
                                        .description("산업군 이름"),
                                fieldWithPath("data[].industryCode").type(JsonFieldType.STRING)
                                        .description("산업군 코드"),
                                fieldWithPath("data[].iconUrl").type(JsonFieldType.STRING).optional()
                                        .description("아이콘 URL"),
                                fieldWithPath("data[].description").type(JsonFieldType.STRING).optional()
                                        .description("산업군 상세 설명"),
                                fieldWithPath("data[].displayOrder").type(JsonFieldType.NUMBER)
                                        .description("노출 순서"),
                                fieldWithPath("data[].active").type(JsonFieldType.BOOLEAN)
                                        .description("활성화 여부"),
                                fieldWithPath("error").type(JsonFieldType.STRING).optional()
                                        .description("에러 메시지 (성공 시 생략됨)")
                        )
                ));
    }

    @Test
    @DisplayName("권한 없는 회원이 산업군 전체 조회 시도시 403 반환")
    public void getAll_member_forbidden () throws Exception {
        authenticate(1L, UserRole.MEMBER);

        mockMvc.perform(get("/api/v1/admin/industries"))
                .andExpect(status().isForbidden())
                .andDo(document("industry-list-forbidden",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부 (false)"),
                                fieldWithPath("error").type(JsonFieldType.STRING)
                                        .description("에러 메시지 (접근 권한 없음)")
                        )
                ));

        verify(service, never()).getAll();
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