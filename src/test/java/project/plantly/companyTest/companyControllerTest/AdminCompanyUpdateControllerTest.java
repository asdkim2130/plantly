package project.plantly.companyTest.companyControllerTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import project.plantly.companyTest.support.CompanyApiDocs;
import project.plantly.domain.company.controller.AdminCompanyController;
import project.plantly.domain.company.dto.AdminCompanySubscriptionResponse;
import project.plantly.domain.company.dto.AdminSubscriptionUpdateRequest;
import project.plantly.domain.company.dto.CompanyUpdateRequest;
import project.plantly.domain.company.enums.CompanyVisibility;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.enums.SubscriptionStatus;
import project.plantly.domain.company.service.CompanyQueryService;
import project.plantly.domain.company.service.CompanyService;
import project.plantly.domain.company.service.CompanyUpdateService;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;
import project.plantly.global.security.UserPrincipal;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 관리자 수정 API 컨트롤러 슬라이스 테스트 — 라우팅/응답 형태 + @PreAuthorize 권한 게이트(ADMIN)를 검증하고,
// 성공/권한 예외 응답을 REST Docs 스니펫으로 문서화한다. 요청/응답 형태는 유저 수정과 동일해 같은 디스크립터를 공유한다.
@ActiveProfiles("test")
@WebMvcTest(controllers = AdminCompanyController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import(AdminCompanyUpdateControllerTest.MethodSecurityTestConfig.class)
public class AdminCompanyUpdateControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {}

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private CompanyUpdateService companyUpdateService;

    @MockitoBean
    private CompanyService companyService;

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
    @DisplayName("관리자가 기본 정보를 수정하면 200 ok 를 반환하고 관리자 경로 서비스에 위임한다")
    void updateBasicInfoByAdmin_success() throws Exception {
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(patch("/api/v1/admin/companies/{id}", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"introTitle\":\"관리자 수정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andDo(document("admin-company-update",
                        pathParameters(parameterWithName("id").description("회사 ID")),
                        requestFields(CompanyApiDocs.companyUpdateRequestFields()),
                        responseFields(CompanyApiDocs.okResponseFields())));

        verify(companyUpdateService).updateBasicInfoByAdmin(eq(5L), any(CompanyUpdateRequest.class));
    }

    @Test
    @DisplayName("관리자가 아닌 유저가 기본 정보 수정을 호출하면 @PreAuthorize 가 막아 403 을 반환한다")
    void updateBasicInfoByAdmin_forbidden_forNonAdmin() throws Exception {
        authenticate(2L, UserRole.MEMBER);

        mockMvc.perform(patch("/api/v1/admin/companies/{id}", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"introTitle\":\"관리자 수정\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("접근 권한이 없습니다."))
                .andDo(document("admin-company-update-forbidden",
                        responseFields(CompanyApiDocs.errorResponseFields())));
    }

    @Test
    @DisplayName("관리자가 공개/비공개를 전환하면 200 ok 를 반환하고 관리자 경로 서비스에 위임한다")
    void changeVisibilityByAdmin_success() throws Exception {
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(patch("/api/v1/admin/companies/{id}/visibility", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andDo(document("admin-company-update-visibility",
                        pathParameters(parameterWithName("id").description("회사 ID")),
                        requestFields(CompanyApiDocs.companyVisibilityUpdateRequestFields()),
                        responseFields(CompanyApiDocs.okResponseFields())));

        verify(companyUpdateService).changeVisibilityByAdmin(eq(5L), eq(CompanyVisibility.PUBLIC));
    }

    @Test
    @DisplayName("관리자가 아닌 유저가 공개/비공개 전환을 호출하면 @PreAuthorize 가 막아 403 을 반환한다")
    void changeVisibilityByAdmin_forbidden_forNonAdmin() throws Exception {
        authenticate(2L, UserRole.MEMBER);

        mockMvc.perform(patch("/api/v1/admin/companies/{id}/visibility", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"PRIVATE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("관리자가 태그를 전체 교체하면 200 ok 를 반환한다")
    void replaceTagsByAdmin_success() throws Exception {
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(put("/api/v1/admin/companies/{id}/tags", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"정밀\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("admin-company-update-tags",
                        pathParameters(parameterWithName("id").description("회사 ID")),
                        requestFields(CompanyApiDocs.replaceListRequestFields("교체할 태그명 목록 (빈 배열이면 전부 비움)")),
                        responseFields(CompanyApiDocs.okResponseFields())));

        verify(companyUpdateService).replaceTagsByAdmin(eq(5L), any());
    }

    @Test
    @DisplayName("관리자가 아닌 유저가 태그 교체를 호출하면 @PreAuthorize 가 막아 403 을 반환한다")
    void replaceTagsByAdmin_forbidden_forNonAdmin() throws Exception {
        authenticate(2L, UserRole.MEMBER);

        mockMvc.perform(put("/api/v1/admin/companies/{id}/tags", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"정밀\"]"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("관리자가 구독을 조회하면 200 과 구독 정보(감사 타임스탬프 포함)를 반환한다")
    void getSubscriptionByAdmin_success() throws Exception {
        AdminCompanySubscriptionResponse response = new AdminCompanySubscriptionResponse(
                5L, "플랜틀리테크", CompanyGrade.PREMIUM, CompanyGrade.PREMIUM, SubscriptionStatus.ACTIVE,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                LocalDateTime.of(2026, 1, 1, 9, 0), LocalDateTime.of(2026, 7, 7, 15, 0));
        given(companyQueryService.getSubscriptionForAdmin(eq(5L))).willReturn(response);
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/admin/companies/{id}/subscription", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyId").value(5L))
                .andExpect(jsonPath("$.data.companyName").value("플랜틀리테크"))
                .andExpect(jsonPath("$.data.grade").value("PREMIUM"))
                .andExpect(jsonPath("$.data.effectiveGrade").value("PREMIUM"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andDo(document("admin-company-subscription",
                        pathParameters(parameterWithName("id").description("회사 ID")),
                        responseFields(CompanyApiDocs.adminCompanySubscriptionResponseFields())));
    }

    @Test
    @DisplayName("관리자가 구독을 수정(grade/status/expiresAt)하면 200 ok 를 반환하고 서비스에 위임한다")
    void updateSubscriptionByAdmin_success() throws Exception {
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(patch("/api/v1/admin/companies/{id}/subscription", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":\"PREMIUM\",\"status\":\"ACTIVE\",\"expiresAt\":\"2026-12-31\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andDo(document("admin-company-subscription-update",
                        pathParameters(parameterWithName("id").description("회사 ID")),
                        requestFields(CompanyApiDocs.adminSubscriptionUpdateRequestFields()),
                        responseFields(CompanyApiDocs.okResponseFields())));

        verify(companyUpdateService).updateSubscriptionByAdmin(eq(5L), any(AdminSubscriptionUpdateRequest.class));
    }

    @Test
    @DisplayName("등급/상태를 누락하면 400(@NotNull 위반) 을 반환한다")
    void updateSubscriptionByAdmin_missingRequiredField_validationError() throws Exception {
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(patch("/api/v1/admin/companies/{id}/subscription", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expiresAt\":\"2026-12-31\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("관리자가 아닌 유저가 구독 수정을 호출하면 @PreAuthorize 가 막아 403 을 반환한다")
    void updateSubscriptionByAdmin_forbidden_forNonAdmin() throws Exception {
        authenticate(2L, UserRole.MEMBER);

        mockMvc.perform(patch("/api/v1/admin/companies/{id}/subscription", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":\"PREMIUM\",\"status\":\"ACTIVE\",\"expiresAt\":null}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("접근 권한이 없습니다."))
                .andDo(document("admin-company-subscription-update-forbidden",
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
