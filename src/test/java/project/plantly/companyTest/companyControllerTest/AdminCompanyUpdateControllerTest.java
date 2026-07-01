package project.plantly.companyTest.companyControllerTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
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
import project.plantly.domain.company.controller.AdminCompanyController;
import project.plantly.domain.company.dto.CompanyUpdateRequest;
import project.plantly.domain.company.service.CompanyQueryService;
import project.plantly.domain.company.service.CompanyService;
import project.plantly.domain.company.service.CompanyUpdateService;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;
import project.plantly.global.security.UserPrincipal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 관리자 수정 API 컨트롤러 슬라이스 테스트 — 라우팅/응답 형태 + @PreAuthorize 권한 게이트(ADMIN)를 검증한다.
@ActiveProfiles("test")
@WebMvcTest(controllers = AdminCompanyController.class)
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
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
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
                .andExpect(jsonPath("$.data").doesNotExist());

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
                .andExpect(jsonPath("$.error").value("접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("관리자가 태그를 전체 교체하면 200 ok 를 반환한다")
    void replaceTagsByAdmin_success() throws Exception {
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(put("/api/v1/admin/companies/{id}/tags", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"정밀\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

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
