package project.plantly.companyTest.companyControllerTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import project.plantly.companyTest.support.CompanyApiDocs;
import project.plantly.companyTest.support.CompanyCreateRequestSamples;
import project.plantly.companyTest.support.CompanyResponseSamples;
import project.plantly.domain.company.controller.AdminCompanyController;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.service.CompanyQueryService;
import project.plantly.domain.company.service.CompanyService;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;
import project.plantly.global.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 관리자 등록 컨트롤러 슬라이스 테스트 (REST Docs 문서화 목적, 성공 케이스 중심).
@ActiveProfiles("test")
@WebMvcTest(controllers = AdminCompanyController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import(AdminCompanyControllerTest.MethodSecurityTestConfig.class)
public class AdminCompanyControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {}

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompanyService companyService;

    // 컨트롤러가 상세 조회용으로 주입받는 협력 객체. 등록 슬라이스 테스트에서는 사용하지 않지만 컨텍스트 로딩을 위해 모킹한다.
    @MockitoBean
    private CompanyQueryService companyQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .build();
    }

    @Test
    @DisplayName("관리자가 회사를 등록하면 201 Created 와 생성된 회사 id 를 반환한다")
    void createCompanyByAdmin_success() throws Exception {
        CompanyCreateRequest request = CompanyCreateRequestSamples.full();
        given(companyService.createByAdmin(eq(1L), any(CompanyCreateRequest.class))).willReturn(200L);
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(post("/api/v1/admin/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회사 등록이 완료되었습니다."))
                .andExpect(jsonPath("$.data.id").value(200L))
                .andDo(document("admin-company-create",
                        requestFields(CompanyApiDocs.companyCreateRequestFields()),
                        responseFields(CompanyApiDocs.idResponseFields())));
    }

    @Test
    @DisplayName("관리자가 아닌 유저가 회사 등록을 호출하면 @PreAuthorize 가 막아 403 을 반환한다")
    void createCompanyByAdmin_forbidden_forNonAdmin() throws Exception {
        authenticate(2L, UserRole.MEMBER);
        // 본문은 유효해야 @Valid 바인딩(400)이 아닌 @PreAuthorize(403) 가 결과를 결정한다.
        mockMvc.perform(post("/api/v1/admin/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CompanyCreateRequestSamples.full())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("접근 권한이 없습니다."))
                .andDo(document("admin-company-create-forbidden",
                        responseFields(CompanyApiDocs.errorResponseFields())));
    }

    @Test
    @DisplayName("관리자 조회는 상태(미연동/삭제) 무관하게 profile + meta 전체를 반환한다")
    void getCompanyByAdmin_success() throws Exception {
        given(companyQueryService.getForAdmin(5L)).willReturn(CompanyResponseSamples.fullDetail());
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/admin/companies/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile.companyName").value("플랜틀리테크"))
                .andExpect(jsonPath("$.data.meta.businessNumber").value("1234567890"))
                .andDo(document("admin-company-detail",
                        pathParameters(parameterWithName("id").description("회사 ID")),
                        responseFields(CompanyApiDocs.companyDetailResponseFields())));
    }

    @Test
    @DisplayName("관리자가 아닌 유저가 관리자 조회를 호출하면 @PreAuthorize 가 막아 403(FORBIDDEN) 를 반환한다")
    void getCompanyByAdmin_forbidden_forNonAdmin() throws Exception {
        authenticate(2L, UserRole.MEMBER);

        mockMvc.perform(get("/api/v1/admin/companies/{id}", 5L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("접근 권한이 없습니다."))
                .andDo(document("admin-company-detail-forbidden",
                        responseFields(CompanyApiDocs.errorResponseFields())));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, UserRole role) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authOf(userId, role));
        SecurityContextHolder.setContext(securityContext);
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
