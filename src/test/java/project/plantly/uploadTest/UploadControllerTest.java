package project.plantly.uploadTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 컨트롤러 슬라이스. 다른 컨트롤러 테스트와 같이 MockMvc 에 시큐리티 필터를 걸지 않는다(springSecurity() 미적용) -
// 걸면 슬라이스에 뜨는 것은 우리 SecurityConfig 가 아니라 부트의 기본 체인이라, permitAll/authenticated 규칙은
// 검증되지 않으면서 CSRF 403 만 얻는다. 여기서 보는 것은 상태 코드·본문·헤더이고, 인증 주체는 직접 심는다.
@ActiveProfiles("test")
@WebMvcTest(controllers = UploadController.class)
@DisplayName("업로드 컨트롤러")
class UploadControllerTest {

    private static final String KEY = "0123456789abcdef0123456789abcdef.png";

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UploadService uploadService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
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
                        .file(new MockMultipartFile("file", "photo.png", "image/png", ImageBytes.png())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("파일이 업로드되었습니다."))
                .andExpect(jsonPath("$.data.url").value("/api/v1/files/" + KEY))
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andExpect(jsonPath("$.data.size").value(1234));
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
                .andExpect(jsonPath("$.error").value("JPG, PNG, WebP 이미지만 업로드할 수 있습니다."));
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
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable"));
    }

    @Test
    @DisplayName("없는 파일은 404 를 봉투에 담아 돌려준다")
    void serve_notFound() throws Exception {
        willThrow(new BusinessException(UploadErrorCode.FILE_NOT_FOUND))
                .given(uploadService).load(KEY);

        mockMvc.perform(get("/api/v1/files/{key}", KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("존재하지 않는 파일입니다."));
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
