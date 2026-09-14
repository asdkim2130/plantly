package project.plantly.globalTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.unit.DataSize;
import org.springframework.web.context.WebApplicationContext;
import project.plantly.domain.upload.ImageFormat;
import project.plantly.domain.upload.storage.StorageProperties;
import project.plantly.global.meta.MetaUploadController;

import java.util.Arrays;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 업로드 제약 조회의 계약.
 *
 * <p>상한은 설정값이라 <b>일부러 기본값(10MB)이 아닌 값을 주입</b>해 확인한다 — 응답이 상수를 읽는 게
 * 아니라 실제 설정을 따라온다는 것이 이 API 의 존재 이유이므로, 기본값으로 테스트하면 하드코딩과
 * 구분이 안 된다.
 *
 * <p>형식 목록은 {@link ImageFormat} 에서 도출하는지를 본다. 기대값을 문자열로 적으면 "목록이 enum 을
 * 따라온다"를 아무도 확인하지 않게 되므로 enum 을 순회해 비교한다.
 */
@ActiveProfiles("test")
@WebMvcTest(controllers = MetaUploadController.class)
@ExtendWith(RestDocumentationExtension.class)
@DisplayName("업로드 제약 조회 API")
class MetaUploadControllerTest {

    // 기본값(10MB)과 다른 값. "설정을 따라온다"를 이 차이로 확인한다.
    private static final long CONFIGURED_BYTES = 3 * 1024 * 1024;

    @TestConfiguration
    static class Config {
        @Bean
        StorageProperties storageProperties() {
            return new StorageProperties(DataSize.ofBytes(CONFIGURED_BYTES), null);
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withResponseDefaults(prettyPrint()))
                .build();
    }

    @Test
    @DisplayName("설정된 파일 상한(바이트)과 허용 형식 목록을 반환한다")
    void getUploadConstraints_success() throws Exception {
        String[] contentTypes = Arrays.stream(ImageFormat.values())
                .map(ImageFormat::getContentType).toArray(String[]::new);
        String[] extensions = Arrays.stream(ImageFormat.values())
                .flatMap(format -> format.getAcceptedExtensions().stream()).toArray(String[]::new);

        mockMvc.perform(get("/api/v1/meta/upload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // 상수가 아니라 주입된 설정을 읽는다.
                .andExpect(jsonPath("$.data.maxFileSizeBytes").value(CONFIGURED_BYTES))
                // 목록은 ImageFormat 선언 순서 그대로 도출된다.
                .andExpect(jsonPath("$.data.allowedContentTypes").value(org.hamcrest.Matchers.contains(contentTypes)))
                .andExpect(jsonPath("$.data.allowedExtensions").value(org.hamcrest.Matchers.contains(extensions)))
                // 저장 확장자(jpg 하나)가 아니라 입력 허용 목록이 나간다. 이게 빠지면 서버는 받는
                // .jpeg 파일을 프론트 필터가 막는다 — 도출 비교만으로는 잡히지 않아 문자열로 못 박는다.
                .andExpect(jsonPath("$.data.allowedExtensions").value(org.hamcrest.Matchers.hasItems("jpeg", "jfif")))
                .andDo(document("meta-upload",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("data.maxFileSizeBytes").type(JsonFieldType.NUMBER)
                                        .description("한 장당 최대 용량(바이트). File.size 와 그대로 비교한다. "
                                                + "환경변수로 배포마다 조정되므로 하드코딩하지 말 것. 서버의 multipart 상한도 이 값에서 "
                                                + "도출되므로 이 크기 이하는 용량 때문에 거절되지 않는다. 넘는 파일은 올리기 전에 막을 것 — "
                                                + "조금 넘으면 400(FILE_TOO_LARGE)이지만 크게 넘으면 서버가 연결을 닫아 응답을 받지 못한다. "
                                                + "\"10MB\" 같은 사람이 읽는 표기는 화면이 만든다"),
                                fieldWithPath("data.allowedContentTypes").type(JsonFieldType.ARRAY)
                                        .description("허용 MIME 타입. <input accept> 에 그대로 쓴다. 서버는 클라이언트가 "
                                                + "보낸 Content-Type 을 믿지 않고 실제 바이트 시그니처로 판별한다"),
                                fieldWithPath("data.allowedExtensions").type(JsonFieldType.ARRAY)
                                        .description("입력으로 받는 확장자(소문자, 점 없음). 한 형식에 여럿일 수 있다"
                                                + "(JPEG: jpg·jpeg·jpe·jfif). 파일명과 비교할 때는 대소문자를 무시하고, "
                                                + "<input accept> 에 쓸 때는 앞에 점을 붙인다. 서버는 파일명을 보지 않고 바이트로 "
                                                + "최종 판정하므로 프론트 사전 검사는 서버가 받을 파일을 거부하지 않는 선에서만 쓴다"),
                                fieldWithPath("code").type(JsonFieldType.STRING).optional()
                                        .description("에러 코드(ErrorCode 상수명). 클라이언트는 문구가 아니라 이 값으로 분기한다. 성공 응답에는 없다"),
                                fieldWithPath("error").type(JsonFieldType.STRING).optional()
                                        .description("오류 메시지 (성공 시 생략)")
                        )));
    }
}
