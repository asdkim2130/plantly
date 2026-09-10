package project.plantly.globalTest;

import io.restassured.filter.cookie.CookieFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import project.plantly.AcceptanceTest;
import project.plantly.domain.upload.ImageFormat;
import project.plantly.domain.upload.storage.StorageProperties;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * 업로드 제약 조회의 인수 테스트.
 *
 * <p>슬라이스 테스트는 시큐리티 필터체인을 붙이지 않아 "로그인이 필요하다" 를 확인하지 못하고,
 * {@code StorageProperties} 를 대역으로 세워 실제 설정이 바인딩되는지도 확인하지 못한다. 둘을 여기서 본다.
 *
 * <p>기대 상한을 숫자로 적지 않고 주입된 {@code StorageProperties} 와 대조한다 — 숫자를 적으면
 * "응답이 설정을 따라온다"를 아무도 확인하지 않게 되고, 이 API 를 만든 이유가 바로 그 추적성이다.
 */
@DisplayName("업로드 제약 조회 - 인증과 실제 설정")
class MetaUploadAcceptanceTest extends AcceptanceTest {

    @Autowired
    private StorageProperties storageProperties;

    // 업로드 자체가 로그인을 요구하므로 제약도 그때 주면 된다.
    // (선택지 카탈로그는 반대로 비로그인 허용 — 공개 상세가 라벨을 쓰기 때문이다)
    @Test
    @DisplayName("익명 사용자는 401 을 받는다")
    void anonymous_isUnauthorized() {
        given()
                .when()
                .get("/api/v1/meta/upload")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("로그인하면 실제 설정에서 바인딩된 상한과 ImageFormat 에서 도출한 형식 목록을 받는다")
    void authenticated_returnsConfiguredLimits() {
        CookieFilter cookies = new CookieFilter();
        String csrf = issueCsrfToken(cookies);
        signUp(cookies, csrf, "upload-meta@plantly.com");
        login(cookies, csrf, "upload-meta@plantly.com", VALID_PASSWORD, false);

        given()
                .filter(cookies)
                .when()
                .get("/api/v1/meta/upload")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                // 설정(app.upload.max-file-size)이 그대로 계약에 실린다. 상수 대조가 아니라 바인딩 확인이다.
                .body("data.maxFileSizeBytes", equalTo((int) storageProperties.maxFileSize().toBytes()))
                .body("data.allowedContentTypes", contains(
                        ImageFormat.JPEG.getContentType(),
                        ImageFormat.PNG.getContentType(),
                        ImageFormat.WEBP.getContentType()))
                .body("data.allowedExtensions", contains(
                        ImageFormat.JPEG.getExtension(),
                        ImageFormat.PNG.getExtension(),
                        ImageFormat.WEBP.getExtension()));
    }
}
