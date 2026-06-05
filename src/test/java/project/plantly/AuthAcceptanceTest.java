package project.plantly;

import io.restassured.filter.cookie.CookieFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import project.plantly.domain.user.User;
import project.plantly.domain.user.UserRepository;
import project.plantly.domain.user.enums.UserStatus;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;


// 세션 기반 인증의 전체 흐름을 실제 서버(RANDOM_PORT) + 실제 필터 체인으로 검증하는 인수 테스트.
// CSRF가 켜져 있어 모든 변경(POST) 요청은 GET /api/v1/auth/csrf 로 받은 토큰을 헤더로 함께 보내야 한다.
class AuthAcceptanceTest extends AcceptanceTest {

    // 인증이 필요한 임의의 보호 자원(핸들러는 없음). 미인증이면 401, 인증되면 인가 게이트를 통과해 404가 된다.
    private static final String PROTECTED_PATH = "/api/v1/users/me";
    private static final String VALID_PASSWORD = "Password1!"; // 10자 + 특수문자 (검증 규칙 통과)

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 → 로그인(세션 발급) → 보호 자원 접근 통과 → 로그아웃 → 세션 만료 전체 흐름")
    void fullAuthFlow() {
        CookieFilter cookies = new CookieFilter();
        String csrf = issueCsrfToken(cookies);

        // 1) 회원가입
        signUp(cookies, csrf, "flow@example.com")
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        // 2) 로그인: 200 + 사용자 정보 5개 + JSESSIONID 발급
        Response loginRes = login(cookies, csrf, "flow@example.com", VALID_PASSWORD, false);

        loginRes.then()
                .statusCode(200)
                .body("email", equalTo("flow@example.com"))
                .body("name", equalTo("홍길동"))
                .body("userStatus", equalTo("ACTIVE"))
                .body("userGrade", equalTo("BASIC"));

        assertThat(loginRes.cookie("JSESSIONID")).isNotNull();

        // 3) 세션 쿠키로 보호 자원 접근 → 인증 게이트 통과(401 아님)
        given().filter(cookies)
                .when()
                .get(PROTECTED_PATH)
                .then()
                .statusCode(not(equalTo(401)));

        // 4) 로그아웃: 200 성공 응답
        given().filter(cookies).header("X-XSRF-TOKEN", csrf)
                .when()
                .post("/api/v1/auth/logout")
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        // 5) 로그아웃 후 같은 보호 자원 → 다시 401
        given().filter(cookies)
                .when()
                .get(PROTECTED_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("미인증 상태로 보호 자원 접근 시 401과 UNAUTHORIZED 메시지(EntryPoint)")
    void accessProtectedWithoutAuth() {
        given()
                .when()
                .get(PROTECTED_PATH)
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("error", equalTo("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인하면 401과 INVALID_CREDENTIALS 메시지")
    void loginWithWrongPassword() {
        CookieFilter cookies = new CookieFilter();
        String csrf = issueCsrfToken(cookies);
        signUp(cookies, csrf, "wrong@example.com").then().statusCode(200);

        login(cookies, csrf, "wrong@example.com", "Wrongpass1!", false)
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("error", equalTo("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 401과 INVALID_CREDENTIALS 메시지")
    void loginWithUnknownEmail() {
        CookieFilter cookies = new CookieFilter();
        String csrf = issueCsrfToken(cookies);

        login(cookies, csrf, "ghost@example.com", VALID_PASSWORD, false)
                .then()
                .statusCode(401)
                .body("error", equalTo("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("정지된 계정으로 로그인하면 403과 ACCOUNT_SUSPENDED 메시지")
    void loginWithSuspendedAccount() {
        User suspended = User.builder()
                .email("suspended@example.com")
                .password(passwordEncoder.encode(VALID_PASSWORD))
                .name("정지유저")
                .phone("01099998888")
                .userStatus(UserStatus.SUSPENDED)
                .build();
        userRepository.save(suspended);

        CookieFilter cookies = new CookieFilter();
        String csrf = issueCsrfToken(cookies);

        login(cookies, csrf, "suspended@example.com", VALID_PASSWORD, false)
                .then()
                .statusCode(403)
                .body("error", equalTo("계정이 비활성화 되었습니다."));
    }

    @Test
    @DisplayName("remember=true로 로그인하면 remember-me 쿠키가 발급된다")
    void loginWithRememberMe() {
        CookieFilter cookies = new CookieFilter();
        String csrf = issueCsrfToken(cookies);
        signUp(cookies, csrf, "remember@example.com").then().statusCode(200);

        Response res = login(cookies, csrf, "remember@example.com", VALID_PASSWORD, true);

        res.then()
           .statusCode(200);

        assertThat(res.cookie("remember-me")).isNotNull();
    }

    @Test
    @DisplayName("CSRF 토큰 없이 변경(POST) 요청하면 403(AccessDeniedHandler)")
    void postWithoutCsrfIsRejected() {
        CookieFilter cookies = new CookieFilter();
        String csrf = issueCsrfToken(cookies);
        signUp(cookies, csrf, "nocsrf@example.com").then().statusCode(200);

        // CSRF 쿠키/헤더 없이 로그인 시도
        String body = """
                {"email":"nocsrf@example.com","password":"%s","remember":false}
                """.formatted(VALID_PASSWORD);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(403)
                .body("error", equalTo("접근 권한이 없습니다."));
    }

    // ---- helpers ----

    private String issueCsrfToken(CookieFilter cookies) {
        return given()
                .filter(cookies)
                .when()
                .get("/api/v1/auth/csrf")
                .then()
                .statusCode(200)
                .extract()
                .cookie("XSRF-TOKEN");
    }

    private Response signUp(CookieFilter cookies, String csrf, String email) {
        String body = """
                {"email":"%s","password":"%s","name":"홍길동","phone":"01012345678"}
                """.formatted(email, VALID_PASSWORD);

        return given()
                .filter(cookies)
                .header("X-XSRF-TOKEN", csrf)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/users/sign-up");
    }

    private Response login(CookieFilter cookies, String csrf, String email, String password, boolean remember) {
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