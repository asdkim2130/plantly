package project.plantly.uploadTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import project.plantly.domain.upload.ImageFormat;
import project.plantly.domain.upload.UploadController;
import project.plantly.domain.upload.UploadService;
import project.plantly.domain.upload.dto.UploadResponse;
import project.plantly.domain.upload.exception.UploadErrorCode;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;
import project.plantly.global.exception.BusinessException;
import project.plantly.global.security.UserPrincipal;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 컨트롤러 슬라이스. 다른 컨트롤러 테스트와 같이 MockMvc 에 시큐리티 필터를 걸지 않는다(springSecurity() 미적용) -
// 걸면 슬라이스에 뜨는 것은 우리 SecurityConfig 가 아니라 부트의 기본 체인이라, permitAll/authenticated 규칙은
// 검증되지 않으면서 CSRF 403 만 얻는다. 여기서 보는 것은 상태 코드·본문·헤더이고, 인증 주체는 직접 심는다.
@ActiveProfiles("test")
@WebMvcTest(controllers = UploadController.class)
@ExtendWith(RestDocumentationExtension.class)
@DisplayName("업로드 컨트롤러")
class UploadControllerTest {

    private static final String KEY = "0123456789abcdef0123456789abcdef.png";

    // 문서에 실리는 요청 본문. 진짜 이미지 바이트를 쓰면 스니펫에 제어 문자가 그대로 찍힌다 - 형식 판별은
    // 서비스 몫이라(모킹됨) 이 슬라이스에서 내용은 아무 역할이 없으므로 읽을 수 있는 자리표시자로 둔다.
    private static final byte[] DOC_IMAGE_PLACEHOLDER = "<이미지 바이너리>".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UploadService uploadService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withResponseDefaults(prettyPrint()))
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("이미지를 올리면 201 과 조회 URL 을 돌려준다 - 이 url 이 회사 요청의 이미지 필드로 그대로 들어간다")
    void upload_success() throws Exception {
        authenticate(7L);
        given(uploadService.upload(eq(7L), any()))
                .willReturn(new UploadResponse("/api/v1/files/" + KEY, "image/png", 1234L));

        mockMvc.perform(multipart("/api/v1/uploads")
                        .file(new MockMultipartFile("file", "photo.png", "image/png", DOC_IMAGE_PLACEHOLDER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("파일이 업로드되었습니다."))
                .andExpect(jsonPath("$.data.url").value("/api/v1/files/" + KEY))
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andExpect(jsonPath("$.data.size").value(1234))
                .andDo(document("upload",
                        requestParts(partWithName("file").description(
                                "이미지 파일 1장. 형식은 파일명·Content-Type 이 아니라 실제 바이트로 판별한다. "
                                        + "한 장당 용량·허용 형식은 업로드 제약 조회(GET /api/v1/meta/upload)를 본다")),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data.url").type(JsonFieldType.STRING).description(
                                        "조회 URL(상대경로). 회사 요청의 logoUrl · coverImageUrl · images[].imageUrl 에 이 문자열을 그대로 담는다"),
                                fieldWithPath("data.contentType").type(JsonFieldType.STRING).description(
                                        "서버가 바이트로 판별한 형식. 클라이언트가 보낸 Content-Type 과 다를 수 있다"),
                                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("저장된 크기(바이트)"))));
    }

    @Test
    @DisplayName("file 파트가 없으면 400 이다 - 500 으로 새지 않는다(required=false 로 받는 이유)")
    void upload_withoutFilePart() throws Exception {
        authenticate(7L);
        willThrow(new BusinessException(UploadErrorCode.FILE_REQUIRED))
                .given(uploadService).upload(eq(7L), isNull());

        mockMvc.perform(multipart("/api/v1/uploads"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("업로드할 파일이 없습니다."));
    }

    @Test
    @DisplayName("허용하지 않는 형식은 400 과 허용 목록을 담은 안내로 나간다")
    void upload_unsupportedType() throws Exception {
        authenticate(7L);
        willThrow(new BusinessException(UploadErrorCode.UNSUPPORTED_IMAGE_TYPE))
                .given(uploadService).upload(eq(7L), any());

        mockMvc.perform(multipart("/api/v1/uploads")
                        .file(new MockMultipartFile("file", "x.png", "image/png", ImageBytes.notAnImage())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("JPG, PNG, WebP 이미지만 업로드할 수 있습니다."))
                .andDo(document("upload-unsupported-type", responseFields(errorResponseFields())));
    }

    @Test
    @DisplayName("multipart 가 아닌 요청(JSON 등)은 415 다 - 서버 탓(500)으로 보이면 안 된다")
    void upload_rejectsNonMultipartRequest() throws Exception {
        authenticate(7L);

        mockMvc.perform(post("/api/v1/uploads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"file\":\"oops\"}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("지원하지 않는 요청 형식입니다."));
    }

    @Test
    @DisplayName("Content-Type 이 아예 없는 요청도 415 다")
    void upload_rejectsRequestWithoutContentType() throws Exception {
        authenticate(7L);

        mockMvc.perform(post("/api/v1/uploads"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("이미지 서빙은 봉투 없이 바이너리를 그대로 흘려보내고 영구 캐시 헤더를 붙인다")
    void serve_success() throws Exception {
        byte[] bytes = ImageBytes.png();
        given(uploadService.load(KEY))
                .willReturn(new UploadService.StoredFile(new ByteArrayResource(bytes), ImageFormat.PNG));

        mockMvc.perform(get("/api/v1/files/{key}", KEY))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(bytes))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable"))
                // 본문은 바이너리라 문서에는 요청·경로·헤더만 싣는다(index.adoc 이 http-response 를 포함하지 않는다).
                .andDo(document("file-serve",
                        pathParameters(parameterWithName("key").description(
                                "업로드 응답 url 의 마지막 경로 조각. 클라이언트가 조립하지 않고 url 을 그대로 쓴다")),
                        responseHeaders(
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("저장 시 판별된 이미지 형식"),
                                headerWithName(HttpHeaders.CACHE_CONTROL).description(
                                        "영구 캐시. 같은 URL 의 내용은 바뀌지 않는다(교체 = 새 업로드 = 새 URL)"))));
    }

    @Test
    @DisplayName("없는 파일은 404 를 봉투에 담아 돌려준다")
    void serve_notFound() throws Exception {
        willThrow(new BusinessException(UploadErrorCode.FILE_NOT_FOUND))
                .given(uploadService).load(KEY);

        mockMvc.perform(get("/api/v1/files/{key}", KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("존재하지 않는 파일입니다."))
                .andDo(document("file-not-found", responseFields(errorResponseFields())));
    }

    private static FieldDescriptor[] errorResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부 (항상 false)"),
                fieldWithPath("code").type(JsonFieldType.STRING).description(
                        "에러 코드(ErrorCode 상수명). 클라이언트는 문구가 아니라 이 값으로 분기한다"),
                fieldWithPath("error").type(JsonFieldType.STRING).description("에러 메시지")
        };
    }

    private void authenticate(Long userId) {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "userStatus", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "userRole", UserRole.MEMBER);
        UserPrincipal principal = new UserPrincipal(user);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
