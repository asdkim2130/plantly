package project.plantly.companyTest.certificationTest;

import project.plantly.domain.company.certification.CertificationExceptionError;
import project.plantly.global.exception.BusinessException;
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
import project.plantly.domain.company.certification.CertificationController;
import project.plantly.domain.company.certification.CertificationService;
import project.plantly.domain.company.certification.CertificationType;
import project.plantly.domain.company.certification.dto.CertificationAdminResponse;
import project.plantly.domain.company.certification.dto.CertificationCreateRequest;
import project.plantly.domain.company.certification.dto.CertificationPublicResponse;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ActiveProfiles("test")
@WebMvcTest(controllers = CertificationController.class)
@ExtendWith(RestDocumentationExtension.class)   // REST Docs: 스니펫 생성 컨텍스트 제공
@Import(CertificationControllerTest.MethodSecurityTestConfig.class)
public class CertificationControllerTest {

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
    private CertificationService service;

    @Test
    @DisplayName("관리자가 인증 생성시 201 Created와 id 반환")
    public void create_admin_success () throws Exception {
        CertificationCreateRequest request = new CertificationCreateRequest(
                "ISO 9001", "iso-9001", CertificationType.MANAGEMENT_SYSTEM, 0);

        given(service.createCertification(any(CertificationCreateRequest.class))).willReturn(1L);
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(post("/api/v1/admin/certifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("인증 항목이 등록되었습니다."))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andDo(document("certification-create",
                        requestFields(
                                fieldWithPath("name").type(JsonFieldType.STRING)
                                        .description("인증 이름"),
                                fieldWithPath("slug").type(JsonFieldType.STRING)
                                        .description("인증 슬러그 (영문/숫자/하이픈)"),
                                fieldWithPath("type").type(JsonFieldType.STRING)
                                        .description("인증 구분 (MANAGEMENT_SYSTEM: 경영시스템, INDUSTRY_SPECIFIC: 산업특화, MARKET_ACCESS: 시장진입)"),
                                fieldWithPath("displayOrder").type(JsonFieldType.NUMBER).optional()
                                        .description("노출 순서 (0 이상, 미입력 시 마지막 순번 + 1 자동 부여)")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING)
                                        .description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT)
                                        .description("생성된 인증 정보"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                        .description("생성된 인증 ID"),
                                fieldWithPath("error").type(JsonFieldType.STRING).optional()
                                        .description("에러 메시지 (성공 시 응답에서 생략됨)")
                        )
                ));
    }

    @Test
    @DisplayName("권한 없는 회원이 인증 생성 시도시 403 반환, 서비스 호출되지 않음")
    public void create_member_forbidden () throws Exception {
        CertificationCreateRequest request = new CertificationCreateRequest(
                "ISO 9001", "iso-9001", CertificationType.MANAGEMENT_SYSTEM, null);
        authenticate(1L, UserRole.MEMBER);

        mockMvc.perform(post("/api/v1/admin/certifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andDo(document("certification-create-forbidden",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부 (false)"),
                                fieldWithPath("error").type(JsonFieldType.STRING)
                                        .description("에러 메시지 (접근 권한 없음)")
                        )
                ));

        verify(service, never()).createCertification(any());
    }

    @Test
    @DisplayName("이름이 비어 있으면 400에러와 검증 에러 반환")
    public void create_blankName_validationFail() throws Exception {
        CertificationCreateRequest request = new CertificationCreateRequest(
                "", "iso-9001", CertificationType.MANAGEMENT_SYSTEM, null);
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(post("/api/v1/admin/certifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists())
                .andDo(document("certification-create-validation-error",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부 (false)"),
                                fieldWithPath("error").type(JsonFieldType.STRING)
                                        .description("검증 실패 메시지 (첫 번째 위반 항목)"),
                                fieldWithPath("errors").type(JsonFieldType.ARRAY).optional()
                                        .description("입력 검증 실패일 때만 존재. 위반 전체가 화면의 폼 순서로 담긴다"),
                                fieldWithPath("errors[].field").type(JsonFieldType.STRING).optional()
                                        .description("위반한 입력칸의 경로. 폼 전체 오류면 생략된다"),
                                fieldWithPath("errors[].message").type(JsonFieldType.STRING).optional()
                                        .description("그 입력칸 아래에 표시할 문구")
                        )
                ));

        verify(service, never()).createCertification(any());
    }

    @Test
    @DisplayName("이름이 중복이면 409를 반환")
    public void create_duplicateName_conflict () throws Exception {
        CertificationCreateRequest request = new CertificationCreateRequest(
                "ISO 9001", "iso-9001", CertificationType.MANAGEMENT_SYSTEM, null);
        willThrow(new BusinessException(CertificationExceptionError.DUPLICATE_CERTIFICATION_NAME))
                .given(service).createCertification(any(CertificationCreateRequest.class));

        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(post("/api/v1/admin/certifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("인증 이름은 중복 불가합니다."))
                .andDo(document("certification-create-duplicate-name",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부 (false)"),
                                fieldWithPath("error").type(JsonFieldType.STRING)
                                        .description("에러 메시지 (인증 이름 중복)")
                        )
                ));
    }


    @Test
    @DisplayName("관리자가 인증 전체 조회시 200 OK와 목록 반환")
    public void getAll_admin_success () throws Exception {
        CertificationAdminResponse response = CertificationAdminResponse.builder()
                .id(1L)
                .certificationName("ISO 9001")
                .slug("iso-9001")
                .type(CertificationType.MANAGEMENT_SYSTEM)
                .displayOrder(0)
                .active(true)
                .build();

        given(service.getAll()).willReturn(List.of(response));
        authenticate(1L, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/admin/certifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].certificationName").value("ISO 9001"))
                .andExpect(jsonPath("$.data[0].slug").value("iso-9001"))
                .andExpect(jsonPath("$.data[0].type").value("MANAGEMENT_SYSTEM"))
                .andExpect(jsonPath("$.data[0].displayOrder").value(0))
                .andExpect(jsonPath("$.data[0].active").value(true))
                .andDo(document("certification-list",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).optional()
                                        .description("응답 메시지 (단순 조회는 생략됨)"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY)
                                        .description("인증 목록 (displayOrder 오름차순)"),
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                        .description("인증 ID"),
                                fieldWithPath("data[].certificationName").type(JsonFieldType.STRING)
                                        .description("인증 이름"),
                                fieldWithPath("data[].slug").type(JsonFieldType.STRING)
                                        .description("인증 슬러그"),
                                fieldWithPath("data[].type").type(JsonFieldType.STRING)
                                        .description("인증 구분: MANAGEMENT_SYSTEM(경영시스템), INDUSTRY_SPECIFIC(산업특화), MARKET_ACCESS(시장진입)"),
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
    @DisplayName("공개 인증 목록은 인증 없이 200 과 type 포함 목록을 반환한다")
    public void getPublicList_success () throws Exception {
        given(service.getPublicList()).willReturn(List.of(
                new CertificationPublicResponse(1L, "ISO 9001 (품질경영)", "iso-9001", CertificationType.MANAGEMENT_SYSTEM),
                new CertificationPublicResponse(6L, "KC 인증", "kc", CertificationType.MARKET_ACCESS)));
        // authenticate() 를 호출하지 않는다 — 로그인 없이 접근 가능해야 한다.

        mockMvc.perform(get("/api/v1/certifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].certificationName").value("ISO 9001 (품질경영)"))
                .andExpect(jsonPath("$.data[0].slug").value("iso-9001"))
                .andExpect(jsonPath("$.data[0].type").value("MANAGEMENT_SYSTEM"))
                .andExpect(jsonPath("$.data[1].type").value("MARKET_ACCESS"))
                // 운영 필드는 공개 응답에 없다
                .andExpect(jsonPath("$.data[0].displayOrder").doesNotExist())
                .andExpect(jsonPath("$.data[0].active").doesNotExist())
                .andDo(document("certification-public-list",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).optional()
                                        .description("응답 메시지 (단순 조회는 생략됨)"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY)
                                        .description("활성 인증 목록 (displayOrder 오름차순). type 으로 그룹핑해 드롭다운을 구성한다"),
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                        .description("인증 ID. 검색 필터(certificationIds)에 그대로 실어 보낸다"),
                                fieldWithPath("data[].certificationName").type(JsonFieldType.STRING)
                                        .description("인증 이름"),
                                fieldWithPath("data[].slug").type(JsonFieldType.STRING)
                                        .description("인증 슬러그"),
                                fieldWithPath("data[].type").type(JsonFieldType.STRING)
                                        .description("인증 구분: MANAGEMENT_SYSTEM(경영시스템), INDUSTRY_SPECIFIC(산업특화), MARKET_ACCESS(시장진입)"),
                                fieldWithPath("error").type(JsonFieldType.STRING).optional()
                                        .description("에러 메시지 (성공 시 생략됨)")
                        )
                ));
    }

    @Test
    @DisplayName("권한 없는 회원이 인증 전체 조회 시도시 403 반환")
    public void getAll_member_forbidden () throws Exception {
        authenticate(1L, UserRole.MEMBER);

        mockMvc.perform(get("/api/v1/admin/certifications"))
                .andExpect(status().isForbidden())
                .andDo(document("certification-list-forbidden",
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