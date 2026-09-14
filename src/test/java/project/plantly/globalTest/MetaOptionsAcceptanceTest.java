package project.plantly.globalTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import project.plantly.AcceptanceTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * 선택지 카탈로그의 인수 테스트.
 *
 * <p>슬라이스 테스트는 시큐리티 필터체인을 붙이지 않아 "비로그인도 읽을 수 있다" 를 확인하지 못한다.
 * 그 한 가지를 여기서 본다.
 *
 * <p>제약 조회({@link MetaConstraintsAcceptanceTest})와 반대라는 점이 이 테스트의 요지다. 제약은 폼 전용이라
 * 401 이지만, 선택지의 라벨은 공개 상세 화면이 값을 그리는 데도 쓴다 — 공개 상세는 익명에게 열려 있으므로
 * 여기 인증을 걸면 로그인한 사람에게만 "양산 적용 가능" 이 보이고 익명에게는 "MASS_PRODUCTION" 이 보인다.
 */
@DisplayName("선택지 카탈로그 - 비로그인 접근")
class MetaOptionsAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("익명 사용자도 선택지와 라벨을 받는다 (공개 상세가 값을 그려야 하므로)")
    void anonymous_canReadCatalog() {
        given()
                .when()
                .get("/api/v1/meta/options")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.trlLevel[0].value", equalTo("PROTOTYPE"))
                .body("data.trlLevel[0].label", equalTo("프로토타입"))
                .body("data.pricingType[0].label", equalTo("고정 단가제"));
    }
}
