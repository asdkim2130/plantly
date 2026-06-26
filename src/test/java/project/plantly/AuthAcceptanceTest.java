package project.plantly;

import io.restassured.filter.cookie.CookieFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import project.plantly.domain.user.User;
import project.plantly.domain.user.repository.UserRepository;
import project.plantly.domain.user.enums.UserStatus;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;


// 세션 기반 인증의 전체 흐름을 실제 서버(RANDOM_PORT) + 실제 필터 체인으로 검증하는 인수 테스트.
// CSRF가 켜져 있어 모든 변경(POST) 요청은 GET /api/v1/auth/csrf 로 받은 토큰을 헤더로 함께 보내야 한다.
class AuthAcceptanceTest extends AcceptanceTest {

    // 인증이 필요한 임의의 보호 자원(핸들러는 없음). 미인증이면 401, 인증되면 인가 게이트를 통과해 404가 된다.
    private static final String PROTECTED_PATH = "/api/v1/users/me";

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
                .statusCode(201)
                .body("success", equalTo(true));

        // 2) 로그인: 200 + ApiResponse(data에 사용자 정보) + JSESSIONID 발급
        Response loginRes = login(cookies, csrf, "flow@example.com", VALID_PASSWORD, false);

        loginRes.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.email", equalTo("flow@example.com"))
                .body("data.name", equalTo("홍길동"))
                .body("data.userStatus", equalTo("ACTIVE"))
                .body("data.userGrade", equalTo("FREE"));

        assertThat(loginRes.cookie("JSESSIONID")).isNotNull();

        // 3) 세션 쿠키로 본인 프로필 조회 → 200 + 인증 주체 본인 정보 반환(IDOR 방지: 외부 id 입력 불가)
        given().filter(cookies)
                .when()
                .get(PROTECTED_PATH)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.email", equalTo("flow@example.com"));

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
        signUp(cookies, csrf, "wrong@example.com").then().statusCode(201);

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
        signUp(cookies, csrf, "remember@example.com").then().statusCode(201);

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
        signUp(cookies, csrf, "nocsrf@example.com").then().statusCode(201);

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
}