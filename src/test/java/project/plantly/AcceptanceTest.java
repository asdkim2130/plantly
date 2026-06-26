package project.plantly;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AcceptanceTest {

    // 인수 테스트 공통 비밀번호 (검증 규칙 통과: 10자 + 특수문자).
    protected static final String VALID_PASSWORD = "Password1!";

    @LocalServerPort
    int port;

    @Autowired
    DatabaseCleanup databaseCleanup;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        databaseCleanup.execute();
    }

    // ---- 세션 인증 헬퍼 (CSRF 토큰 발급 → 회원가입 → 로그인) ----
    // 변경(POST) 요청은 CSRF가 켜져 있어 GET /api/v1/auth/csrf 로 받은 토큰을 헤더로 함께 보내야 한다.
    // GET 조회 자체엔 CSRF가 필요 없지만, 세션을 얻으려면 로그인(POST)을 거쳐야 한다.

    protected String issueCsrfToken(CookieFilter cookies) {
        return given()
                .filter(cookies)
                .when()
                .get("/api/v1/auth/csrf")
                .then()
                .statusCode(200)
                .extract()
                .cookie("XSRF-TOKEN");
    }

    protected Response signUp(CookieFilter cookies, String csrf, String email) {
        String body = """
                {"email":"%s","password":"%s","reWritePassword":"%s","name":"홍길동","phone":"01012345678"}
                """.formatted(email, VALID_PASSWORD, VALID_PASSWORD);

        return given()
                .filter(cookies)
                .header("X-XSRF-TOKEN", csrf)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/users/sign-up");
    }

    protected Response login(CookieFilter cookies, String csrf, String email, String password, boolean remember) {
        String body = """
                {"email":"%s","password":"%s","remember":%b}
                """.formatted(email, password, remember);

        return given()
                .filter(cookies)
                .header("X-XSRF-TOKEN", csrf)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/auth/login");
    }
}