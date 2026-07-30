package project.plantly.companyTest.certificationTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import project.plantly.AcceptanceTest;
import project.plantly.domain.company.certification.Certification;
import project.plantly.domain.company.certification.CertificationRepository;
import project.plantly.domain.company.certification.CertificationType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

/**
 * 공개 인증 옵션 목록의 인수 테스트.
 *
 * <p>슬라이스 테스트({@code CertificationControllerTest})는 시큐리티 필터체인을 붙이지 않아
 * permitAll 설정 자체를 검증하지 못한다. 여기서만 실제 필터체인을 통과시켜 "익명 접근 가능"을 확인한다.
 * 활성 필터도 파생 쿼리 이름에만 의존하지 않고 실제 DB 로 확인한다.
 */
class CertificationAcceptanceTest extends AcceptanceTest {

    @Autowired
    CertificationRepository certificationRepository;

    @Test
    @DisplayName("익명 사용자도 활성 인증 목록을 200 으로 받는다 (검색 필터 패널은 비로그인도 쓴다)")
    void anonymous_returnsActiveCertificationsOnly() {
        certificationRepository.save(
                Certification.create("ISO 9001", "iso-9001", CertificationType.MANAGEMENT_SYSTEM, 0));
        certificationRepository.save(
                Certification.create("KC 인증", "kc", CertificationType.MARKET_ACCESS, 1));

        Certification retired =
                Certification.create("폐기된 인증", "retired", CertificationType.INDUSTRY_SPECIFIC, 2);
        retired.deactivate();
        certificationRepository.save(retired);

        given() // 세션 없음 = 익명
                .when()
                .get("/api/v1/certifications")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                // displayOrder 오름차순, 폐기(active=false) 인증은 옵션에서 제외
                .body("data", hasSize(2))
                .body("data.slug", contains("iso-9001", "kc"))
                .body("data.type", contains("MANAGEMENT_SYSTEM", "MARKET_ACCESS"))
                .body("data.slug", not(hasItem("retired")));
    }
}
