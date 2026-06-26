package project.plantly.companyTest.companyControllerTest;

import io.restassured.filter.cookie.CookieFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import project.plantly.AcceptanceTest;
import project.plantly.companyTest.support.CompanyAggregateSeeder;
import project.plantly.domain.user.User;
import project.plantly.domain.user.enums.UserRole;
import project.plantly.domain.user.enums.UserStatus;
import project.plantly.domain.user.repository.UserRepository;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

// 회사 조회 API 인수 테스트 — 실제 서버(RANDOM_PORT) + 실 DB + 실 필터 체인.
// CompanyAggregateLoader 가 조립한 '진짜' Response 내용, 접근 제어(공개/소유자/관리자), 상태 매핑(404/403/401)을 검증한다.
// 응답 필드의 값 단언은 이 인수 레벨에서 한 번만 한다(슬라이스는 REST Docs 문서화 + 보안 게이트 담당).
class CompanyQueryAcceptanceTest extends AcceptanceTest {

    @Autowired
    private CompanyAggregateSeeder seeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("공개 조회 GET /api/v1/companies/{id}")
    class PublicView {

        @Test
        @DisplayName("익명 사용자도 200 으로 조립된 공개 프로필을 받는다 (내부·운영 정보는 노출되지 않는다)")
        void anonymous_returnsAssembledPublicProfile() {
            CookieFilter owner = new CookieFilter();
            long ownerId = signUpMember(owner, "owner@example.com");
            long companyId = seeder.seedPublishedCompany(ownerId);

            given() // 세션 없음 = 익명
                    .when()
                    .get("/api/v1/companies/{id}", companyId)
                    .then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.id", equalTo((int) companyId))
                    .body("data.companyName", equalTo(CompanyAggregateSeeder.COMPANY_NAME))
                    .body("data.ceoName", equalTo(CompanyAggregateSeeder.CEO_NAME))
                    .body("data.verified", equalTo(true))
                    .body("data.featured", equalTo(true))
                    // 부속: 대표 연락처 / 대표 레퍼런스 + 표지 썸네일 1장 / 갤러리 2장 / 소재·링크 마스터
                    .body("data.representativeContact.contactName", equalTo(CompanyAggregateSeeder.REP_CONTACT_NAME))
                    .body("data.representativeReference.projectTitle", equalTo(CompanyAggregateSeeder.REP_REFERENCE_TITLE))
                    .body("data.representativeReference.thumbnailUrl", equalTo(CompanyAggregateSeeder.REP_THUMBNAIL_URL))
                    .body("data.galleryImages.size()", equalTo(2))
                    .body("data.materialNames", hasItem(CompanyAggregateSeeder.MATERIAL_NAME))
                    .body("data.categories[0].categoryName", equalTo(CompanyAggregateSeeder.CATEGORY_NAME))
                    .body("data.industries[0].industryName", equalTo(CompanyAggregateSeeder.INDUSTRY_NAME))
                    // 공개 경계: 내부·운영 필드는 공개 응답에 존재하지 않는다
                    .body("data.businessNumber", nullValue())
                    .body("data.meta", nullValue());
        }

        @Test
        @DisplayName("소프트 삭제된 회사는 공개 조회에서 404 로 미존재 취급한다")
        void deletedCompany_isNotFound() {
            CookieFilter owner = new CookieFilter();
            long ownerId = signUpMember(owner, "owner-del@example.com");
            long deletedId = seeder.seedDeletedCompany(ownerId);

            given()
                    .when()
                    .get("/api/v1/companies/{id}", deletedId)
                    .then()
                    .statusCode(404)
                    .body("success", equalTo(false))
                    .body("error", notNullValue());
        }

        @Test
        @DisplayName("존재하지 않는 회사 id 는 404 를 반환한다")
        void unknownId_isNotFound() {
            given()
                    .when()
                    .get("/api/v1/companies/{id}", 999999)
                    .then()
                    .statusCode(404)
                    .body("success", equalTo(false));
        }
    }

    @Nested
    @DisplayName("소유자 전용 조회 GET /api/v1/companies/{id}/private")
    class OwnerView {

        @Test
        @DisplayName("회사 멤버 본인은 profile + 내부·운영 meta 를 받는다")
        void member_receivesProfileAndMeta() {
            CookieFilter owner = new CookieFilter();
            long ownerId = signUpMember(owner, "owner-priv@example.com");
            long companyId = seeder.seedPublishedCompany(ownerId);

            given()
                    .filter(owner)
                    .when()
                    .get("/api/v1/companies/{id}/private", companyId)
                    .then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.profile.companyName", equalTo(CompanyAggregateSeeder.COMPANY_NAME))
                    .body("data.meta.businessNumber", equalTo(CompanyAggregateSeeder.BUSINESS_NUMBER))
                    .body("data.meta.registrationSource", equalTo("USER"))
                    .body("data.meta.ownerUserId", equalTo((int) ownerId))
                    .body("data.meta.claimed", equalTo(true))
                    .body("data.meta.deleted", equalTo(false));
        }

        @Test
        @DisplayName("회사 멤버가 아닌 다른 유저가 호출하면 403(COMPANY_ACCESS_DENIED)")
        void nonMember_isForbidden() {
            CookieFilter owner = new CookieFilter();
            long ownerId = signUpMember(owner, "owner-x@example.com");
            long companyId = seeder.seedPublishedCompany(ownerId);

            CookieFilter intruder = new CookieFilter();
            signUpMember(intruder, "intruder@example.com");

            given()
                    .filter(intruder)
                    .when()
                    .get("/api/v1/companies/{id}/private", companyId)
                    .then()
                    .statusCode(403)
                    .body("success", equalTo(false));
        }

        @Test
        @DisplayName("미인증 상태로 호출하면 401")
        void unauthenticated_isUnauthorized() {
            CookieFilter owner = new CookieFilter();
            long ownerId = signUpMember(owner, "owner-unauth@example.com");
            long companyId = seeder.seedPublishedCompany(ownerId);

            given() // 세션 없음
                    .when()
                    .get("/api/v1/companies/{id}/private", companyId)
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("소프트 삭제된 회사도 소유자에게는 200 으로 보인다")
        void deletedCompany_stillVisibleToOwner() {
            CookieFilter owner = new CookieFilter();
            long ownerId = signUpMember(owner, "owner-deleted@example.com");
            long deletedId = seeder.seedDeletedCompany(ownerId);

            given()
                    .filter(owner)
                    .when()
                    .get("/api/v1/companies/{id}/private", deletedId)
                    .then()
                    .statusCode(200)
                    .body("data.meta.deleted", equalTo(true));
        }
    }

    @Nested
    @DisplayName("관리자 조회 GET /api/v1/admin/companies/{id}")
    class AdminView {

        @Test
        @DisplayName("관리자는 소유자 미연동(unclaimed) 회사도 profile + meta 전체를 받는다")
        void admin_receivesUnclaimedCompany() {
            long adminId = createAdminUser("admin@example.com");
            CookieFilter admin = loginExisting("admin@example.com");
            long companyId = seeder.seedAdminRegisteredCompany(adminId);

            given()
                    .filter(admin)
                    .when()
                    .get("/api/v1/admin/companies/{id}", companyId)
                    .then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.profile.companyName", equalTo("관리자등록회사"))
                    .body("data.meta.registrationSource", equalTo("ADMIN"))
                    .body("data.meta.claimed", equalTo(false))
                    .body("data.meta.ownerUserId", nullValue())
                    .body("data.meta.registeredBy", equalTo((int) adminId));
        }

        @Test
        @DisplayName("관리자가 아닌 유저가 호출하면 @PreAuthorize 가 막아 403")
        void nonAdmin_isForbidden() {
            CookieFilter owner = new CookieFilter();
            long ownerId = signUpMember(owner, "member@example.com");
            long companyId = seeder.seedPublishedCompany(ownerId);

            given()
                    .filter(owner) // 일반 멤버 세션
                    .when()
                    .get("/api/v1/admin/companies/{id}", companyId)
                    .then()
                    .statusCode(403)
                    .body("success", equalTo(false));
        }

        @Test
        @DisplayName("미인증 상태로 호출하면 401")
        void unauthenticated_isUnauthorized() {
            given()
                    .when()
                    .get("/api/v1/admin/companies/{id}", 1)
                    .then()
                    .statusCode(401);
        }
    }

    // ---- helpers ----

    // 회원가입 + 로그인까지 마친 멤버 세션을 cookies 에 채우고, 그 유저의 id 를 반환한다.
    private long signUpMember(CookieFilter cookies, String email) {
        String csrf = issueCsrfToken(cookies);
        signUp(cookies, csrf, email).then().statusCode(201);
        login(cookies, csrf, email, VALID_PASSWORD, false).then().statusCode(200);
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    // ADMIN 권한 유저를 DB 에 직접 저장하고 id 를 반환한다(가입 플로우는 MEMBER 만 만들기 때문).
    private long createAdminUser(String email) {
        User admin = User.builder()
                .email(email)
                .password(passwordEncoder.encode(VALID_PASSWORD))
                .name("관리자")
                .phone("01000000000")
                .userStatus(UserStatus.ACTIVE)
                .userRole(UserRole.ADMIN)
                .build();
        return userRepository.save(admin).getId();
    }

    // 이미 저장된 유저로 로그인해 세션 쿠키를 반환한다.
    private CookieFilter loginExisting(String email) {
        CookieFilter cookies = new CookieFilter();
        String csrf = issueCsrfToken(cookies);
        login(cookies, csrf, email, VALID_PASSWORD, false).then().statusCode(200);
        return cookies;
    }
}
