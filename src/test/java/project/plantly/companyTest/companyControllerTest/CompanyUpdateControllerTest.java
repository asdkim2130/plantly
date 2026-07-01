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
import project.plantly.domain.company.controller.CompanyController;
import project.plantly.domain.company.dto.CompanyUpdateRequest;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.service.CompanyQueryService;
import project.plantly.domain.company.service.CompanyService;
import project.plantly.domain.company.service.CompanyUpdateService;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;
import project.plantly.global.exception.BusinessException;
import project.plantly.global.security.UserPrincipal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 유저(소유자) 수정 API 컨트롤러 슬라이스 테스트 — 라우팅/검증/인가 매핑/응답 형태(ok())를 서비스 목킹으로 검증한다.
// 실제 수정 동작(전체 교체·삭제 스코프·검색 재동기화)은 실DB 통합 테스트(후속)가 담당한다.
@ActiveProfiles("test")
@WebMvcTest(controllers = CompanyController.class)
@Import(CompanyUpdateControllerTest.MethodSecurityTestConfig.class)
public class CompanyUpdateControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {}

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private CompanyUpdateService companyUpdateService;

    // 컨트롤러가 함께 주입받는 협력 객체. 이 테스트에서 직접 쓰진 않지만 컨텍스트 로딩을 위해 모킹한다.
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
    @DisplayName("소유자가 기본 정보를 부분 수정하면 200 과 본문 없는 성공 응답(ok)만 반환한다")
    void updateBasicInfo_success() throws Exception {
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(patch("/api/v1/companies/{id}", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"introTitle\":\"새 한줄 요약\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist());

        verify(companyUpdateService).updateBasicInfoByUser(eq(9L), eq(7L), any(CompanyUpdateRequest.class));
    }

    @Test
    @DisplayName("필수 필드를 빈 문자열로 비우려 하면 400(@Size(min=1) 위반) 을 반환한다")
    void updateBasicInfo_blankRequiredField_validationError() throws Exception {
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(patch("/api/v1/companies/{id}", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("소유자가 아닌 유저가 수정하면 403(COMPANY_ACCESS_DENIED) 를 반환한다")
    void updateBasicInfo_notOwner_forbidden() throws Exception {
        authenticate(7L, UserRole.MEMBER);
        willThrow(new BusinessException(CompanyErrorCode.COMPANY_ACCESS_DENIED))
                .given(companyUpdateService).updateBasicInfoByUser(eq(9L), eq(7L), any(CompanyUpdateRequest.class));

        mockMvc.perform(patch("/api/v1/companies/{id}", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"introTitle\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("해당 회사에 대한 접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("존재하지 않는 회사를 수정하면 404(COMPANY_NOT_FOUND) 를 반환한다")
    void updateBasicInfo_notFound() throws Exception {
        authenticate(7L, UserRole.MEMBER);
        willThrow(new BusinessException(CompanyErrorCode.COMPANY_NOT_FOUND))
                .given(companyUpdateService).updateBasicInfoByUser(eq(404L), eq(7L), any(CompanyUpdateRequest.class));

        mockMvc.perform(patch("/api/v1/companies/{id}", 404L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"introTitle\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("존재하지 않는 회사입니다."));
    }

    @Test
    @DisplayName("소유자가 태그를 전체 교체하면 200 ok 를 반환하고 서비스에 위임한다")
    void replaceTags_success() throws Exception {
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(put("/api/v1/companies/{id}/tags", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"정밀\",\"자동화\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(companyUpdateService).replaceTagsByUser(eq(9L), eq(7L), any());
    }

    @Test
    @DisplayName("소유자가 카테고리 링크를 전체 교체하면 200 ok 를 반환한다")
    void replaceCategories_success() throws Exception {
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(put("/api/v1/companies/{id}/categories", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(companyUpdateService).replaceCategoriesByUser(eq(9L), eq(7L), any());
    }

    @Test
    @DisplayName("갤러리에 DETAIL 외 타입이 섞이면 400(GALLERY_IMAGE_TYPE_NOT_ALLOWED) 를 반환한다")
    void replaceGalleryImages_nonDetailType_badRequest() throws Exception {
        authenticate(7L, UserRole.MEMBER);
        willThrow(new BusinessException(CompanyErrorCode.GALLERY_IMAGE_TYPE_NOT_ALLOWED))
                .given(companyUpdateService).replaceGalleryImagesByUser(eq(9L), eq(7L), any());

        mockMvc.perform(put("/api/v1/companies/{id}/images", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"imageUrl\":\"https://cdn/x.png\",\"imageType\":\"PROJECT\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("갤러리에는 상세 이미지(DETAIL)만 등록할 수 있습니다."));
    }

    @Test
    @DisplayName("연락처를 1건 초과로 교체하면 400(CONTACT_LIMIT_EXCEEDED) 를 반환한다")
    void replaceContacts_overLimit_badRequest() throws Exception {
        authenticate(7L, UserRole.MEMBER);
        willThrow(new BusinessException(CompanyErrorCode.CONTACT_LIMIT_EXCEEDED))
                .given(companyUpdateService).replaceContactsByUser(eq(9L), eq(7L), any());

        mockMvc.perform(put("/api/v1/companies/{id}/contacts", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"contactName\":\"a\"},{\"contactName\":\"b\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("연락처는 1건만 등록할 수 있습니다."));
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
