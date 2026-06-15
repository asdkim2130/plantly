package project.plantly.companyTest.categoryTest;

import project.plantly.domain.company.category.dto.CategoryTreeResponse;
import project.plantly.domain.company.category.exception.CategoryErrorException;
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
import project.plantly.domain.company.category.CategoryAdminController;
import project.plantly.domain.company.category.CategoryAdminService;
import project.plantly.domain.company.category.dto.CategoryCreateRequest;
import project.plantly.domain.company.category.tree.CategoryTreeService;
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
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;


@ActiveProfiles("test")
@WebMvcTest(controllers = CategoryAdminController.class)
@ExtendWith(RestDocumentationExtension.class)   // REST Docs: 스니펫 생성 컨텍스트 제공
@Import(CategoryAdminControllerTest.MethodSecurityTestConfig.class)
public class CategoryAdminControllerTest {

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
    private CategoryAdminService service;
    @MockitoBean
    private CategoryTreeService treeService;

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
                .andExpect(status().isForbidden())
                .andDo(document("category-create-forbidden",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부 (false)"),
                                fieldWithPath("error").type(JsonFieldType.STRING)
                                        .description("에러 메시지 (접근 권한 없음)")
                        )
                ));

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
                .andExpect(jsonPath("$.error").exists())
                .andDo(document("category-create-validation-error",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부 (false)"),
                                fieldWithPath("error").type(JsonFieldType.STRING)
                                        .description("검증 실패 메시지 (첫 번째 위반 항목)")
                        )
                ));


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
                .andExpect(jsonPath("$.error").value("카테고리 코드는 중복 불가입니다."))
                .andDo(document("category-create-duplicate-code",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부 (false)"),
                                fieldWithPath("error").type(JsonFieldType.STRING)
                                        .description("에러 메시지 (카테고리 코드 중복)")
                        )
                ));

    }

    @Test
    @DisplayName("관리자가 카테고리 트리 조회시 200 OK와 중첩 트리 반환")
    public void getTree_admin_success () throws Exception {
        CategoryTreeResponse child = CategoryTreeResponse.builder()
                .id(2L)
                .categoryCode("child-category")
                .categoryName("자식 카테고리")
                .iconUrl("icon")
                .description("설명")
                .depth(2)
                .displayOrder(0)
                .children(List.of())
                .build();

        CategoryTreeResponse root = CategoryTreeResponse.builder()
                .id(1L)
                .categoryCode("root-category")
                .categoryName("루트 카테고리")
                .iconUrl("icon")
                .description("설명")
                .depth(1)
                .displayOrder(0)
                .children(List.of(child))
                .build();

        given(service.getTree()).willReturn(List.of(root));
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].categoryCode").value("root-category"))
                .andExpect(jsonPath("$.data[0].children[0].id").value(2))
                .andExpect(jsonPath("$.data[0].children[0].categoryName").value("자식 카테고리"))
                .andDo(document("category-tree",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).optional()
                                        .description("응답 메시지 (단순 조회는 생략됨)"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY)
                                        .description("최상위(루트) 카테고리 목록"),
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                        .description("카테고리 ID"),
                                fieldWithPath("data[].categoryCode").type(JsonFieldType.STRING)
                                        .description("카테고리 코드"),
                                fieldWithPath("data[].categoryName").type(JsonFieldType.STRING)
                                        .description("카테고리 이름"),
                                fieldWithPath("data[].iconUrl").type(JsonFieldType.STRING).optional()
                                        .description("아이콘 URL"),
                                fieldWithPath("data[].description").type(JsonFieldType.STRING).optional()
                                        .description("카테고리 상세 설명"),
                                fieldWithPath("data[].depth").type(JsonFieldType.NUMBER)
                                        .description("트리 깊이 (루트=1)"),
                                fieldWithPath("data[].displayOrder").type(JsonFieldType.NUMBER)
                                        .description("형제 간 노출 순서"),
                                fieldWithPath("data[].active").type(JsonFieldType.BOOLEAN)
                                        .description("활성화 여부"),
                                // 자식은 동일 구조가 재귀되므로 subsection 으로 묶어 문서화
                                subsectionWithPath("data[].children").type(JsonFieldType.ARRAY)
                                        .description("하위 카테고리 목록 (동일 구조 재귀)"),
                                fieldWithPath("error").type(JsonFieldType.STRING).optional()
                                        .description("에러 메시지 (성공 시 생략됨)")
                        )
                ));

    }

    @Test
    @DisplayName("권한 없는 회원이 카테고리 트리 조회 시도시 403 반환")
    public void getTree_member_forbidden ()throws Exception {
        authenticate(1L, UserRole.MEMBER);

        mockMvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isForbidden())
                .andDo(document("category-tree-forbidden",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부 (false)"),
                                fieldWithPath("error").type(JsonFieldType.STRING)
                                        .description("에러 메세지 (접근 권한 없음)")
                        )
                ));

        verify(service, never()).getTree();
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
