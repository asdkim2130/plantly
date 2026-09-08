package project.plantly.globalTest;

import io.restassured.filter.cookie.CookieFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import project.plantly.AcceptanceTest;
import project.plantly.domain.company.dto.CompanyConstraints;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

/**
 * 폼 제약 조회의 인수 테스트.
 *
 * <p>슬라이스 테스트는 시큐리티 필터체인을 붙이지 않아 "로그인이 필요하다" 를 확인하지 못하고,
 * reader 를 대역으로 두어 실제 DTO 에서 값이 나오는지도 확인하지 못한다. 두 가지를 여기서만 본다.
 *
 * <p>실제 컨텍스트를 띄우므로 {@code FormConstraintsReader} 가 스프링이 만든 검증 팩토리를 그대로
 * 받아 쓰는지도 여기서 걸린다 — 검증 실패 응답의 문구를 만드는 것과 같은 팩토리여야 두 응답의 문구가 같다.
 */
@DisplayName("폼 제약 조회 - 인증과 실제 값")
class MetaConstraintsAcceptanceTest extends AcceptanceTest {

    // 등록·수정 폼은 로그인 후에만 열리므로 제약도 그때 주면 된다.
    @Test
    @DisplayName("익명 사용자는 401 을 받는다")
    void anonymous_isUnauthorized() {
        given()
                .when()
                .get("/api/v1/meta/constraints/my-company-create")
                .then()
                .statusCode(401);
    }

    // 응답의 숫자가 CompanyConstraints 를 실제로 따라오는지 — 이 왕복이 성립해야 프론트가 저장 전에
    // 서버와 같은 기준으로 막을 수 있다.
    @Test
    @DisplayName("로그인하면 요청 DTO 에서 뽑은 규칙을 받는다")
    void authenticated_returnsRulesFromRequestDto() {
        CookieFilter cookies = new CookieFilter();
        String csrf = issueCsrfToken(cookies);
        signUp(cookies, csrf, "constraints@plantly.com");
        login(cookies, csrf, "constraints@plantly.com", VALID_PASSWORD, false);

        given()
                .filter(cookies)
                .when()
                .get("/api/v1/meta/constraints/my-company-create")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.form", equalTo("my-company-create"))
                // 폼 순서 = DTO 선언 순서. 자가등록의 첫 칸은 선행 인증 식별자다.
                .body("data.fields[0].field", equalTo("verificationId"))
                .body("data.fields[0].rules[0].type", equalTo("required"))
                // 메시지 템플릿이 아니라 해석된 문구가 나가야 한다.
                .body("data.fields[0].rules[0].message", not(startsWith("{")))
                .body("data.fields[1].field", equalTo("companyName"))
                .body("data.fields[1].rules[1].type", equalTo("maxLength"))
                .body("data.fields[1].rules[1].value", equalTo(CompanyConstraints.COMPANY_NAME_MAX));
    }
}
