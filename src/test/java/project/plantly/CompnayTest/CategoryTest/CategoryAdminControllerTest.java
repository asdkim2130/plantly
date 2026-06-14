package project.plantly.CompnayTest.CategoryTest;

import project.plantly.domain.company.category.exception.CategoryErrorException;
import project.plantly.global.exception.BusinessException;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import project.plantly.domain.company.category.CategoryAdminController;
import project.plantly.domain.company.category.CategoryAdminService;
import project.plantly.domain.company.category.dto.CategoryCreateRequest;
import project.plantly.domain.company.category.tree.CategoryTreeService;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;
import project.plantly.global.security.UserPrincipal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ActiveProfiles("test")
@WebMvcTest(controllers = CategoryAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CategoryAdminControllerTest.MethodSecurityTestConfig.class)
@AutoConfigureRestDocs
public class CategoryAdminControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {}

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CategoryAdminService service;
    @MockitoBean
    CategoryTreeService treeService;

    @Test
    @DisplayName("관리자가 카테고리 생성시 200Ok와 id 반환")
    public void create_admin_success () throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest(null, "a", "a", "url", "상세설명", 0);

        given(service.create(any(CategoryCreateRequest.class))).willReturn(1L);
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(post("/api/v1/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("카테고리 생성이 완료되었습니다."))
                .andExpect(jsonPath("$.data").value(1L))
                .andDo(document("category-create",
                        requestFields(
                                fieldWithPath("parentId").type(JsonFieldType.NUMBER).optional()
                                        .description("상위 카테고리 ID (최상위면 null)"),
                                fieldWithPath("categoryName").type(JsonFieldType.STRING)
                                        .description("카테고리 이름"),
                                fieldWithPath("categoryCode").type(JsonFieldType.STRING)
                                        .description("카테고리 코드 (영문/숫자/하이픈)"),
                                fieldWithPath("iconUrl").type(JsonFieldType.STRING).optional()
                                        .description("아이콘 URL"),
                                fieldWithPath("description").type(JsonFieldType.STRING).optional()
                                        .description("카테고리 상세 설명"),
                                fieldWithPath("displayOrder").type(JsonFieldType.NUMBER).optional()
                                        .description("노출 순서")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING)
                                        .description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NUMBER)
                                        .description("생성된 카테고리 ID"),
                                fieldWithPath("error").type(JsonFieldType.STRING).optional()
                                        .description("에러 메시지 (성공 시 응답에서 생략됨)")
                        )
                ));
    }

    @Test
    @DisplayName("권한 없는 회원이 카테고리 생성 시도시 403 반환, 서비스 호출되지 않음")
    public void create_member_forbidden () throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest(null, "a", "a", null, null, null);
        authenticate(1L, UserRole.MEMBER);

        mockMvc.perform(post("/api/v1/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(service, never()).create(any());

    }

    @Test
    @DisplayName("categoryCode에 한글이 들어가면 400에러와 검증 에러 반환")
    public void create_invalidCod_validationFail() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest(null, "한글", "한글", null, null, null);
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(post("/api/v1/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());


    }

    @Test
    @DisplayName("코드가 중복이면 409를 반환")
    public void create_duplicateCode_conflict ()throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest(null, "a", "a", null, null, null);
        willThrow(new BusinessException(CategoryErrorException.DUPLICATE_CATEGORY_CODE))
                .given(service).create(request);

        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(post("/api/v1/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("카테고리 코드는 중복 불가입니다."));

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
