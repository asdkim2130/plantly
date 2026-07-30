package project.plantly.companyTest.countryTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import project.plantly.AcceptanceTest;
import project.plantly.domain.company.country.Continent;
import project.plantly.domain.company.country.Country;
import project.plantly.domain.company.country.CountryRepository;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

/**
 * 공개 국가 옵션 목록의 인수 테스트.
 *
 * <p>슬라이스 테스트({@code CountryControllerTest})는 시큐리티 필터체인을 붙이지 않아 permitAll
 * 설정 자체를 검증하지 못한다. 여기서만 실제 필터체인을 통과시켜 "익명 접근 가능"을 확인한다.
 *
 * <p>정렬도 함께 확인하지만, test 프로파일은 H2(Java String 비교)라 여기서 걸리는 것은 "정렬이
 * 적용된다"까지다. 정렬을 DB {@code ORDER BY} 로 되돌리면 H2 에서는 이 테스트가 그대로 통과한다 —
 * 운영 DB(Postgres)의 collation 과 갈리는 지점은 {@code CountryOrderingPostgresTest} 가 잡는다.
 */
class CountryAcceptanceTest extends AcceptanceTest {

    @Autowired
    CountryRepository countryRepository;

    @Test
    @DisplayName("익명 사용자도 전체 국가 목록을 200 으로 받고, 국가명 한글 오름차순으로 정렬된다")
    void anonymous_returnsAllCountriesSortedByKoreanName() {
        // 저장 순서를 가나다순과 어긋나게 섞어 넣어, 정렬이 쿼리에서 실제로 일어나는지 확인한다.
        countryRepository.save(Country.create("VN", "VNM", "704", "베트남", "Viet Nam", Continent.ASIA));
        countryRepository.save(Country.create("DE", "DEU", "276", "독일", "Germany", Continent.EUROPE));
        countryRepository.save(Country.create("GH", "GHA", "288", "가나", "Ghana", Continent.AFRICA));
        countryRepository.save(Country.create("KR", "KOR", "410", "대한민국", "South Korea", Continent.ASIA));
        // 남극은 수출국으로 고를 일이 없지만 목록에서 빠지지 않는다 (Country 에 active 가 없다).
        countryRepository.save(Country.create("AQ", "ATA", "010", "남극", "Antarctica", Continent.ANTARCTICA));

        given() // 세션 없음 = 익명
                .when()
                .get("/api/v1/countries")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", hasSize(5))
                // 가(U+AC00) < 남(U+B0A8) < 대(U+B300) < 독(U+B3C5) < 베(U+BCA0)
                .body("data.nameKo", contains("가나", "남극", "대한민국", "독일", "베트남"))
                .body("data.code", contains("GH", "AQ", "KR", "DE", "VN"))
                // 프론트가 group by 할 대륙 값이 실려 나간다 (평면 목록 + continent)
                .body("data.continent", contains("AFRICA", "ANTARCTICA", "ASIA", "EUROPE", "ASIA"))
                // 드롭다운에 쓰이지 않는 식별자는 응답에 없다
                .body("data[0].alpha3", nullValue())
                .body("data[0].numericCode", nullValue());
    }
}
