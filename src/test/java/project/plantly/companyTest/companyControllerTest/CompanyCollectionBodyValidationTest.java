package project.plantly.companyTest.companyControllerTest;

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
import project.plantly.domain.company.service.CompanyDraftService;
import project.plantly.domain.company.service.CompanyQueryService;
import project.plantly.domain.company.service.CompanyRegistrationService;
import project.plantly.domain.company.service.CompanyStatsService;
import project.plantly.domain.company.service.CompanyUpdateService;
import project.plantly.domain.company.service.CompanyVerificationService;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;
import project.plantly.global.security.UserPrincipal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컬렉션 본문에 깨진 원소(`[null]`)가 들어왔을 때 500(NPE)이 아니라 400 으로 끊기는지 검증.
 *
 * <p>{@code @Valid} 는 <b>null 원소 자체를 거부하지 않는다</b>는 게 이 테스트의 요지다. non-null 원소
 * 안으로는 중첩 검증이 들어가므로 {@code certificationId} 의 {@code @NotNull} 은 {@code @Valid} 만으로도
 * 잡힌다. 하지만 원소가 null 이면 검증할 대상이 없어 그대로 통과한다 — 그 자리는 원소에 직접 건
 * {@code List<@NotNull X>}(컨테이너 원소 제약)가 막는다. 실제로 {@code images}/{@code contacts}/
 * {@code references} 는 {@code @Valid} 가 붙어 있는데도 {@code [null]} 이 통과하고 있었다.
 *
 * <p>반대로 <b>컬렉션을 아예 보내지 않는 것은 정상</b>이다. 인증·연락처 등을 하나도 등록하지 않은
 * "해당 사항 없음" 상태를 설계가 허용하므로, 리스트 자체에는 {@code @NotNull} 을 걸지 않는다.
 */
@ActiveProfiles("test")
@WebMvcTest(controllers = CompanyController.class)
@Import(CompanyCollectionBodyValidationTest.MethodSecurityTestConfig.class)
@DisplayName("컬렉션 본문 검증: 깨진 원소는 400, 미선택은 정상")
class CompanyCollectionBodyValidationTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {}

    @Autowired WebApplicationContext context;

    @MockitoBean CompanyRegistrationService companyRegistrationService;
    @MockitoBean CompanyQueryService companyQueryService;
    @MockitoBean CompanyUpdateService companyUpdateService;
    @MockitoBean CompanyVerificationService companyVerificationService;
    @MockitoBean CompanyDraftService companyDraftService;
    @MockitoBean CompanyStatsService companyStatsService;

    // 등록 요청의 필수 항목을 채운 최소 본문. 이 테스트의 관심사는 컬렉션 원소 검증이므로,
    // 그 외 필수 항목(주소 3축)이 빠져 400 이 나면 무엇 때문에 거절됐는지 구분되지 않는다.
    private static final String MINIMAL_BODY = "{\"verificationId\":1,\"companyName\":\"회사\""
            + ",\"postalCode\":\"06236\",\"roadAddress\":\"서울시 강남구 테헤란로 1\",\"detailAddress\":\"10층\"";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        authenticate();
    }

    @Test
    @DisplayName("등록 요청의 certifications 에 null 원소가 있으면 400 이다")
    void createRejectsNullCertificationElement() throws Exception {
        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MINIMAL_BODY + ",\"certifications\":[null]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증을 하나도 고르지 않은 등록 요청은 정상이다 (필드 없음/null/빈 배열)")
    void createAllowsNoCertifications() throws Exception {
        for (String certifications : new String[]{"", ",\"certifications\":null", ",\"certifications\":[]"}) {
            mockMvc.perform(post("/api/v1/companies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(MINIMAL_BODY + certifications + "}"))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @DisplayName("인증 교체(PUT) 본문의 null 원소는 400 이다 — @Valid 는 null 원소를 거부하지 않는다")
    void replaceRejectsNullCertificationElement() throws Exception {
        mockMvc.perform(put("/api/v1/companies/1/certifications")
                        .contentType(MediaType.APPLICATION_JSON).content("[null]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    // 이쪽은 원소가 non-null 이라 @Valid 의 중첩 검증만으로도 걸린다. 위 [null] 케이스와 걸리는 장치가 다르다.
    @DisplayName("인증 교체(PUT) 본문의 certificationId 가 null 이면 400 이다")
    void replaceRejectsNullCertificationId() throws Exception {
        mockMvc.perform(put("/api/v1/companies/1/certifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"certificationId\":null,\"customName\":\"x\"}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증을 전부 비우는 교체(빈 배열)는 정상이다")
    void replaceAllowsEmptyList() throws Exception {
        mockMvc.perform(put("/api/v1/companies/1/certifications")
                        .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("같은 구멍이 있던 이미지·연락처·레퍼런스 교체도 null 원소를 400 으로 끊는다")
    void replaceRejectsNullElementInSiblingCollections() throws Exception {
        for (String path : new String[]{"images", "contacts", "references"}) {
            mockMvc.perform(put("/api/v1/companies/1/" + path)
                            .contentType(MediaType.APPLICATION_JSON).content("[null]"))
                    .andExpect(status().isBadRequest());
        }
    }

    private void authenticate() {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", 7L);
        ReflectionTestUtils.setField(user, "userStatus", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "userRole", UserRole.MEMBER);
        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
    }
}
