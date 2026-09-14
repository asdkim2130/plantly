package project.plantly.uploadTest;

import io.restassured.filter.cookie.CookieFilter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.unit.DataSize;
import project.plantly.AcceptanceTest;
import project.plantly.global.exception.CommonErrorCode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * 업로드 제약 API 가 알려준 상한을 서버가 실제로 지키는지 — 실 서블릿 컨테이너로 확인한다.
 *
 * <p>슬라이스 테스트(MockMvc)는 multipart 상한을 적용하지 않고, 단위 테스트는 설정 빈이 서블릿에 실제로
 * 걸리는지 모른다. 상한이 어긋나는 자리는 서블릿 컨테이너뿐이라 여기서만 잡힌다.
 *
 * <p>상한을 <b>13MB</b> 로 두는 이유: 옛 설정의 요청 상한 기본값(12MB)을 넘는 값이다. 요청 상한을 따로
 * 설정하던 때는 파일 상한만 이렇게 올리면 제약 API 는 13MB 를 알려주는데 서블릿은 12MB 에서 끊었다.
 * 그 배포를 재현해, 상한 바로 그 크기의 파일이 통과하는지 본다.
 */
@DisplayName("업로드 상한 - 제약 API 가 알려준 크기를 서블릿이 실제로 받는다")
class UploadSizeLimitAcceptanceTest extends AcceptanceTest {

    private static final DataSize LEGACY_REQUEST_LIMIT = DataSize.ofMegabytes(12);

    // 실제 저장까지 가므로 프로젝트의 ./uploads 를 더럽히지 않게 임시 디렉터리로 돌린다.
    private static final Path UPLOAD_DIR = createTempDir();

    @DynamicPropertySource
    static void uploadProperties(DynamicPropertyRegistry registry) {
        registry.add("app.upload.max-file-size", () -> "13MB");        registry.add("app.upload.local.base-dir", UPLOAD_DIR::toString);
    }

    @AfterAll
    static void deleteUploadDir() throws IOException {
        FileSystemUtils.deleteRecursively(UPLOAD_DIR);
    }

    @Test
    @DisplayName("제약 API 가 알려준 상한과 정확히 같은 크기의 파일은 업로드된다")
    void fileAtReportedLimit_isAccepted() {
        CookieFilter cookies = new CookieFilter();
        String csrf = loginAs(cookies, "upload-limit-ok@plantly.com");
        long limit = reportedLimit(cookies);

        // 옛 요청 상한을 넘는 상한인지 먼저 못 박는다 — 아니면 이 테스트는 회귀를 재현하지 못한다.
        assertThat(limit).isGreaterThan(LEGACY_REQUEST_LIMIT.toBytes());

        given()
                .filter(cookies)
                .header("X-XSRF-TOKEN", csrf)
                .multiPart("file", "photo.jpg", ImageBytes.jpegOfSize((int) limit), "image/jpeg")
                .when()
                .post("/api/v1/uploads")
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.size", equalTo((int) limit));
    }

    @Test
    @DisplayName("상한을 1바이트라도 넘으면 400 FILE_TOO_LARGE 로 거절한다")
    void fileOverReportedLimit_isRejected() {
        CookieFilter cookies = new CookieFilter();
        String csrf = loginAs(cookies, "upload-limit-over@plantly.com");
        long limit = reportedLimit(cookies);

        given()
                .filter(cookies)
                .header("X-XSRF-TOKEN", csrf)
                .multiPart("file", "photo.jpg", ImageBytes.jpegOfSize((int) limit + 1), "image/jpeg")
                .when()
                .post("/api/v1/uploads")
                .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("code", equalTo(CommonErrorCode.FILE_TOO_LARGE.name()));
    }

    // 로그인 뒤 issueCsrfToken 을 다시 부르면 쿠키가 재발급되지 않아 null 이 되므로 첫 토큰을 계속 쓴다.
    private String loginAs(CookieFilter cookies, String email) {
        String csrf = issueCsrfToken(cookies);
        signUp(cookies, csrf, email);
        login(cookies, csrf, email, VALID_PASSWORD, false);
        return csrf;
    }

    // 기대 상한을 숫자로 적지 않고 프론트가 보는 그 값을 쓴다 — 이 테스트가 확인하는 것은 "알려준 값을 지킨다" 다.
    private long reportedLimit(CookieFilter cookies) {
        return given()
                .filter(cookies)
                .when()
                .get("/api/v1/meta/upload")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getLong("data.maxFileSizeBytes");
    }

    private static Path createTempDir() {
        try {
            return Files.createTempDirectory("plantly-upload-limit-");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
