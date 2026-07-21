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
import project.plantly.domain.company.controller.CompanyController;
import project.plantly.domain.company.dto.CompanySubscriptionResponse;
import project.plantly.domain.company.dto.CompanyVerificationRequest;
import project.plantly.domain.company.dto.CompanyVerificationResponse;
import project.plantly.domain.company.dto.MyCompanyCreateRequest;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.enums.SubscriptionStatus;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.search.CompanySearchCriteria;
import project.plantly.domain.company.search.dto.CompanySummary;
import project.plantly.domain.company.service.CompanyQueryService;
import project.plantly.domain.company.service.CompanyService;
import project.plantly.domain.company.service.CompanyUpdateService;
import project.plantly.domain.company.service.CompanyVerificationService;
import project.plantly.global.PageInfo;
import project.plantly.global.PageResponse;
import project.plantly.global.exception.BusinessException;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;
import project.plantly.global.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 유저 자가등록 컨트롤러 슬라이스 테스트 (REST Docs 문서화 목적, 성공 케이스 중심).
@ActiveProfiles("test")
@WebMvcTest(controllers = CompanyController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import(CompanyControllerTest.MethodSecurityTestConfig.class)
public class CompanyControllerTest {

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

    // 컨트롤러가 수정용으로 주입받는 협력 객체. 이 테스트에서는 사용하지 않지만 컨텍스트 로딩을 위해 모킹한다.
    @MockitoBean
    private CompanyUpdateService companyUpdateService;

    @MockitoBean
    private CompanyVerificationService companyVerificationService;

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
    @DisplayName("사업자 인증에 성공하면 verificationId 와 검증된 값들을 반환한다")
    void verifyBusiness_success() throws Exception {
        CompanyVerificationRequest request = CompanyCreateRequestSamples.verificationRequest();
        given(companyVerificationService.verify(eq(7L), any(CompanyVerificationRequest.class)))
                .willReturn(new CompanyVerificationResponse(99L, "1234567890", "김대표",
                        LocalDate.of(2020, 1, 15), LocalDateTime.of(2026, 7, 19, 12, 30)));
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(post("/api/v1/companies/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationId").value(99L))
                // 하이픈을 넣어 보내도 정규화된 값으로 돌아온다.
                .andExpect(jsonPath("$.data.businessNumber").value("1234567890"))
                .andDo(document("company-verification",
                        requestFields(CompanyApiDocs.verificationRequestFields()),
                        responseFields(CompanyApiDocs.verificationResponseFields())));
    }

    @Test
    @DisplayName("국세청 정보와 일치하지 않으면 400(VERIFICATION_MISMATCH) 을 반환한다")
    void verifyBusiness_mismatch() throws Exception {
        given(companyVerificationService.verify(eq(7L), any(CompanyVerificationRequest.class)))
                .willThrow(new BusinessException(CompanyErrorCode.VERIFICATION_MISMATCH));
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(post("/api/v1/companies/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CompanyCreateRequestSamples.verificationRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andDo(document("company-verification-mismatch",
                        responseFields(CompanyApiDocs.errorResponseFields())));
    }

    @Test
    @DisplayName("국세청 장애 시 503 으로 안내한다 — 입력 오류와 구분되어야 한다")
    void verifyBusiness_ntsUnavailable() throws Exception {
        given(companyVerificationService.verify(eq(7L), any(CompanyVerificationRequest.class)))
                .willThrow(new BusinessException(CompanyErrorCode.VERIFICATION_UNAVAILABLE));
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(post("/api/v1/companies/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CompanyCreateRequestSamples.verificationRequest())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("일일 인증 시도 한도를 넘기면 429 를 반환한다")
    void verifyBusiness_limitExceeded() throws Exception {
        given(companyVerificationService.verify(eq(7L), any(CompanyVerificationRequest.class)))
                .willThrow(new BusinessException(CompanyErrorCode.VERIFICATION_LIMIT_EXCEEDED));
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(post("/api/v1/companies/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CompanyCreateRequestSamples.verificationRequest())))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("인증된 유저가 회사를 등록하면 201 Created 와 생성된 회사 id 를 반환한다")
    void createMyCompany_success() throws Exception {
        MyCompanyCreateRequest request = CompanyCreateRequestSamples.myFull();
        given(companyService.createByUser(eq(7L), any(MyCompanyCreateRequest.class))).willReturn(100L);
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회사 등록이 완료되었습니다."))
                .andExpect(jsonPath("$.data.id").value(100L))
                .andDo(document("company-create",
                        requestFields(CompanyApiDocs.myCompanyCreateRequestFields()),
                        responseFields(CompanyApiDocs.idResponseFields())));
    }

    @Test
    @DisplayName("필수 항목(companyName)이 비어 있으면 400 검증 오류를 반환한다")
    void createMyCompany_validationError() throws Exception {
        authenticate(7L, UserRole.MEMBER);
        // companyName 만 공란(@NotBlank 위반), 나머지는 모두 선택이라 단일 검증 오류만 발생한다.
        String invalidJson = """
                {"companyName":"","ceoName":"김대표"}
                """;

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists())
                .andDo(document("company-create-validation-error",
                        responseFields(CompanyApiDocs.errorResponseFields())));
    }

    @Test
    @DisplayName("등록 정책(등급 한도/마스터 유효성 등)에 걸리면 400(정책 위반 메시지) 를 반환한다")
    void createMyCompany_policyViolation() throws Exception {
        // 정책 위반은 서비스가 BusinessException(400)으로 던진다. 대표로 카테고리 한도 초과를 사용한다.
        given(companyService.createByUser(eq(7L), any(MyCompanyCreateRequest.class)))
                .willThrow(new BusinessException(CompanyErrorCode.CATEGORY_LIMIT_EXCEEDED));
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CompanyCreateRequestSamples.myFull())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("현재 등급에서 선택 가능한 카테고리 개수를 초과했습니다."))
                .andDo(document("company-create-policy-violation",
                        responseFields(CompanyApiDocs.errorResponseFields())));
    }

    @Test
    @DisplayName("공개 상세 조회는 인증 없이도 공개 프로필(meta 제외)을 반환한다")
    void getCompany_public_success() throws Exception {
        given(companyQueryService.getPublic(1L, null)).willReturn(CompanyResponseSamples.fullPublic());

        mockMvc.perform(get("/api/v1/companies/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.companyName").value("플랜틀리테크"))
                .andExpect(jsonPath("$.data.representativeContact.contactName").value("이담당"))
                .andDo(document("company-public-detail",
                        pathParameters(parameterWithName("id").description("회사 ID")),
                        responseFields(CompanyApiDocs.companyPublicResponseFields())));
    }

    @Test
    @DisplayName("소유자 전용 조회는 멤버 본인에게 profile + 내부·운영 meta 를 반환한다")
    void getMyCompany_owner_success() throws Exception {
        given(companyQueryService.getOwnerView(eq(9L), eq(7L)))
                .willReturn(CompanyResponseSamples.fullDetail());
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(get("/api/v1/companies/{id}/private", 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile.companyName").value("플랜틀리테크"))
                .andExpect(jsonPath("$.data.meta.businessNumber").value("1234567890"))
                .andExpect(jsonPath("$.data.meta.ownerUserId").value(7L))
                .andDo(document("company-owner-detail",
                        pathParameters(parameterWithName("id").description("회사 ID")),
                        responseFields(CompanyApiDocs.companyDetailResponseFields())));
    }

    @Test
    @DisplayName("소유자 전용 구독 조회는 멤버 본인에게 구독 정보(등급/유효등급/상태/기간)를 반환한다")
    void getMySubscription_owner_success() throws Exception {
        CompanySubscriptionResponse subscription = new CompanySubscriptionResponse(
                9L, "플랜틀리테크", CompanyGrade.PREMIUM, CompanyGrade.PREMIUM, SubscriptionStatus.ACTIVE,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        given(companyQueryService.getSubscriptionForOwner(eq(9L), eq(7L))).willReturn(subscription);
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(get("/api/v1/companies/{id}/subscription", 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyId").value(9L))
                .andExpect(jsonPath("$.data.companyName").value("플랜틀리테크"))
                .andExpect(jsonPath("$.data.grade").value("PREMIUM"))
                .andExpect(jsonPath("$.data.effectiveGrade").value("PREMIUM"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.startedAt").value("2026-01-01"))
                .andExpect(jsonPath("$.data.expiresAt").value("2026-12-31"))
                .andDo(document("company-subscription",
                        pathParameters(parameterWithName("id").description("회사 ID")),
                        responseFields(CompanyApiDocs.companySubscriptionResponseFields())));
    }

    @Test
    @DisplayName("소유자 전용 구독 조회를 멤버가 아닌 유저가 호출하면 403(COMPANY_ACCESS_DENIED) 를 반환한다")
    void getMySubscription_accessDenied() throws Exception {
        given(companyQueryService.getSubscriptionForOwner(eq(9L), eq(7L)))
                .willThrow(new BusinessException(CompanyErrorCode.COMPANY_ACCESS_DENIED));
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(get("/api/v1/companies/{id}/subscription", 9L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("해당 회사에 대한 접근 권한이 없습니다."))
                .andDo(document("company-subscription-forbidden",
                        responseFields(CompanyApiDocs.errorResponseFields())));
    }

    @Test
    @DisplayName("공개 조회 대상 회사가 없거나 삭제됐으면 404(COMPANY_NOT_FOUND) 를 반환한다")
    void getCompany_notFound() throws Exception {
        given(companyQueryService.getPublic(404L, null))
                .willThrow(new BusinessException(CompanyErrorCode.COMPANY_NOT_FOUND));

        mockMvc.perform(get("/api/v1/companies/{id}", 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("존재하지 않는 회사입니다."))
                .andDo(document("company-public-detail-not-found",
                        responseFields(CompanyApiDocs.errorResponseFields())));
    }

    @Test
    @DisplayName("소유자 전용 조회를 멤버가 아닌 유저가 호출하면 403(COMPANY_ACCESS_DENIED) 를 반환한다")
    void getMyCompany_accessDenied() throws Exception {
        given(companyQueryService.getOwnerView(eq(9L), eq(7L)))
                .willThrow(new BusinessException(CompanyErrorCode.COMPANY_ACCESS_DENIED));
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(get("/api/v1/companies/{id}/private", 9L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("해당 회사에 대한 접근 권한이 없습니다."))
                .andDo(document("company-owner-detail-forbidden",
                        responseFields(CompanyApiDocs.errorResponseFields())));
    }

    @Test
    @DisplayName("공개 회사 목록/검색은 인증 없이 페이징된 요약 카드를 반환한다")
    void searchCompanies_public_success() throws Exception {
        CompanySummary item = new CompanySummary(1L, "플랜틀리", "스마트팜 솔루션",
                "https://cdn/logo.png", "서울 강남구", true, false, true,
                List.of("제조", "정밀가공"), List.of("스마트팜", "IoT"), List.of("농업기술"),
                true, false);   // likedByMe, favoritedByMe
        PageResponse<CompanySummary> page = new PageResponse<>(List.of(item), new PageInfo(1, 20, 1, 1));
        given(companyQueryService.search(any(CompanySearchCriteria.class), any(Pageable.class), isNull())).willReturn(page);

        mockMvc.perform(get("/api/v1/companies?keyword=스마트팜&categoryIds=1&page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].companyName").value("플랜틀리"))
                .andExpect(jsonPath("$.data.content[0].spotlight").value(true))
                .andExpect(jsonPath("$.data.content[0].likedByMe").value(true))
                .andExpect(jsonPath("$.data.content[0].favoritedByMe").value(false))
                .andExpect(jsonPath("$.data.content[0].categoryNames[0]").value("제조"))
                .andExpect(jsonPath("$.data.content[0].tagNames[1]").value("IoT"))
                .andExpect(jsonPath("$.data.content[0].industryNames[0]").value("농업기술"))
                .andExpect(jsonPath("$.data.pageInfo.totalElement").value(1))
                .andDo(document("company-search",
                        queryParameters(CompanyApiDocs.companySearchQueryParameters()),
                        responseFields(CompanyApiDocs.companySearchResponseFields())));
    }

    @Test
    @DisplayName("내 회사 목록은 인증된 본인 소유 회사를 페이징된 요약 카드로 반환한다")
    void getMyCompanies_success() throws Exception {
        CompanySummary item = new CompanySummary(1L, "플랜틀리", "스마트팜 솔루션",
                "https://cdn/logo.png", "서울 강남구", true, false, true,
                List.of("제조", "정밀가공"), List.of("스마트팜", "IoT"), List.of("농업기술"),
                false, false);   // 내 회사 목록은 개인화 미적용
        PageResponse<CompanySummary> page = new PageResponse<>(List.of(item), new PageInfo(1, 20, 1, 1));
        given(companyQueryService.listMyCompanies(eq(7L), any(Pageable.class))).willReturn(page);
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(get("/api/v1/companies/my?page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].companyName").value("플랜틀리"))
                .andExpect(jsonPath("$.data.content[0].categoryNames[0]").value("제조"))
                .andExpect(jsonPath("$.data.content[0].tagNames[1]").value("IoT"))
                .andExpect(jsonPath("$.data.content[0].industryNames[0]").value("농업기술"))
                .andExpect(jsonPath("$.data.pageInfo.totalElement").value(1))
                .andDo(document("company-my",
                        queryParameters(CompanyApiDocs.companyMyQueryParameters()),
                        responseFields(CompanyApiDocs.companySearchResponseFields())));
    }

    @Test
    @DisplayName("내 즐겨찾기 목록은 인증된 본인이 담은 회사를 즐겨찾기순 요약 카드로 반환한다")
    void getMyFavorites_success() throws Exception {
        // 즐겨찾기 목록이므로 favoritedByMe 는 정의상 true. likedByMe 는 카드마다 실제 값이 채워진다.
        CompanySummary item = new CompanySummary(1L, "플랜틀리", "스마트팜 솔루션",
                "https://cdn/logo.png", "서울 강남구", true, false, true,
                List.of("제조", "정밀가공"), List.of("스마트팜", "IoT"), List.of("농업기술"),
                true, true);
        PageResponse<CompanySummary> page = new PageResponse<>(List.of(item), new PageInfo(1, 20, 1, 1));
        given(companyQueryService.listMyFavorites(eq(7L), any(Pageable.class))).willReturn(page);
        authenticate(7L, UserRole.MEMBER);

        mockMvc.perform(get("/api/v1/companies/favorites?page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].companyName").value("플랜틀리"))
                .andExpect(jsonPath("$.data.content[0].favoritedByMe").value(true))
                .andExpect(jsonPath("$.data.content[0].likedByMe").value(true))
                .andExpect(jsonPath("$.data.pageInfo.totalElement").value(1))
                .andDo(document("company-favorites",
                        queryParameters(CompanyApiDocs.companyMyQueryParameters()),
                        responseFields(CompanyApiDocs.companySearchResponseFields())));
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
