package project.plantly.globalTest;

import io.restassured.filter.cookie.CookieFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import project.plantly.AcceptanceTest;
import project.plantly.domain.upload.ImageFormat;
import project.plantly.domain.upload.storage.StorageProperties;
import project.plantly.global.exception.CommonErrorCode;

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
    // 401/403 은 시큐리티 필터단(SecurityResponseWriter)에서 나가 MVC advice 가 닿지 않는다. 그래서 봉투가
    // 갈릴 수 있는 유일한 자리이고, 여기 code 가 빠지면 클라이언트는 "로그인 필요"를 한글 문구로 판별해야 한다
    // (세션 만료 시 재로그인 유도가 그 판정 위에 선다). 실 필터체인을 타는 인수 테스트에서만 확인된다.
    @Test
    @DisplayName("익명 사용자는 401 을 받고, 필터단 응답에도 code(UNAUTHORIZED)가 실린다")
    void anonymous_isUnauthorized() {
        given()
                .when()
                .get("/api/v1/meta/upload")
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("code", equalTo(CommonErrorCode.UNAUTHORIZED.name()))
                .body("error", equalTo(CommonErrorCode.UNAUTHORIZED.getMessage()));
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
