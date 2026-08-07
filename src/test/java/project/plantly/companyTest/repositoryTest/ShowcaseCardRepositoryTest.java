package project.plantly.companyTest.repositoryTest;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.companyTest.support.PostgresContainerTest;
import project.plantly.domain.company.entity.Address;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.enums.CompanyVisibility;
import project.plantly.domain.company.repository.ShowcaseCardRepository;
import project.plantly.domain.company.repository.ShowcaseCardRepository.ShowcaseRail;
import project.plantly.domain.company.search.dto.CompanySummary;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 메인 화면 레일 조회. 핵심 검증은 "노출 자격을 회사 컬럼이 아니라 구독에서 조회 시점에 파생한다" 는 것 —
 * 그래서 구독이 만료되면 아무도 아무것도 끄지 않아도 레일에서 빠져야 한다.
 */
@Transactional
@DisplayName("ShowcaseCardRepository: 메인 화면 노출 레일")
class ShowcaseCardRepositoryTest extends PostgresContainerTest {

    @Autowired EntityManager em;
    @Autowired ShowcaseCardRepository repository;

    private static final LocalDate TODAY = LocalDate.now();
    private static final int SLOTS = 10;   // 자르기와 무관하게 자격만 보고 싶을 때 쓰는 넉넉한 자리 수

    @Test
    @DisplayName("유료 활성 구독(PREMIUM/ENTERPRISE, 미만료)은 pin 없이도 노출된다")
    void activePaidSubscription_isEligibleWithoutPin() {
        Company premium = withSubscription("프리미엄",
                CompanySubscription.active(CompanyGrade.PREMIUM, TODAY.minusDays(10), TODAY.plusYears(1)));
        Company enterprise = withSubscription("엔터프라이즈",
                CompanySubscription.active(CompanyGrade.ENTERPRISE, TODAY.minusDays(10), null)); // 무기한
        em.flush();

        assertThat(premium.isSpotlight()).isFalse();   // 회사 컬럼은 여전히 false — 자격은 저장되지 않는다
        assertThat(enterprise.isSpotlight()).isFalse();
        assertThat(ids(repository.findSpotlight(SLOTS)))
                .containsExactlyInAnyOrder(premium.getId(), enterprise.getId());
    }

    @Test
    @DisplayName("구독이 만료되면 아무것도 끄지 않아도 레일에서 자동으로 빠진다")
    void expiredSubscription_dropsOutAutomatically() {
        Company valid = withSubscription("유효",
                CompanySubscription.active(CompanyGrade.ENTERPRISE, TODAY.minusDays(100), TODAY.plusDays(1)));
        withSubscription("만료",
                CompanySubscription.active(CompanyGrade.ENTERPRISE, TODAY.minusDays(100), TODAY.minusDays(1)));
        em.flush();

        assertThat(ids(repository.findSpotlight(SLOTS))).containsExactly(valid.getId());
    }

    @Test
    @DisplayName("체험(TRIAL)·관리자 등록(ADMIN_EXEMPT)·하위 등급은 자격에서 제외된다")
    void trialAndExemptAndLowerGrades_areNotEligible() {
        // 체험은 등급이 ENTERPRISE 라도 제외 — 유료 전환 동기를 남긴다.
        withSubscription("체험", CompanySubscription.trial(CompanyGrade.ENTERPRISE, TODAY, TODAY.plusDays(30)));
        // 관리자 등록은 grade=ENTERPRISE 가 명목상 값이라 자격으로 읽으면 안 된다(띄우려면 pin).
        withSubscription("관리자등록", CompanySubscription.adminExempt(TODAY));
        // 유료지만 등급 미달.
        withSubscription("스탠다드",
                CompanySubscription.active(CompanyGrade.STANDARD, TODAY, TODAY.plusYears(1)));
        em.flush();

        assertThat(repository.findSpotlight(SLOTS).cards()).isEmpty();
    }

    @Test
    @DisplayName("관리자 고정(pin)은 요금제와 무관하게 노출되고, 자격분보다 앞에 온다")
    void pinnedCompanies_comeFirstRegardlessOfPlan() {
        Company paid = withSubscription("유료",
                CompanySubscription.active(CompanyGrade.ENTERPRISE, TODAY, null));
        Company pinnedSecond = withSubscription("제휴사B", CompanySubscription.freeForUser(TODAY));
        pinnedSecond.turnOnSpotlight(2);
        Company pinnedFirst = withSubscription("제휴사A", CompanySubscription.freeForUser(TODAY));
        pinnedFirst.turnOnSpotlight(1);
        em.flush();

        // pin 먼저(spotlight_order 순), 그다음 요금제 자격분
        assertThat(ids(repository.findSpotlight(SLOTS)))
                .containsExactly(pinnedFirst.getId(), pinnedSecond.getId(), paid.getId());
    }

    @Test
    @DisplayName("삭제되거나 비공개인 회사는 자격이 있어도 노출되지 않는다")
    void deletedOrPrivate_areExcluded() {
        Company visible = withSubscription("공개",
                CompanySubscription.active(CompanyGrade.ENTERPRISE, TODAY, null));
        Company deleted = withSubscription("삭제",
                CompanySubscription.active(CompanyGrade.ENTERPRISE, TODAY, null));
        deleted.delete();
        Company priv = withSubscription("비공개",
                CompanySubscription.active(CompanyGrade.ENTERPRISE, TODAY, null));
        priv.changeVisibility(CompanyVisibility.PRIVATE);
        em.flush();

        assertThat(ids(repository.findSpotlight(SLOTS))).containsExactly(visible.getId());
    }

    @Test
    @DisplayName("후보가 자리보다 많으면 자리 수만큼 자르되, candidateCount 는 자르기 전 총수를 그대로 알려준다")
    void overflow_isTruncatedButCounted() {
        for (int i = 0; i < 4; i++) {
            withSubscription("유료" + i, CompanySubscription.active(CompanyGrade.ENTERPRISE, TODAY, null));
        }
        em.flush();

        ShowcaseRail rail = repository.findSpotlight(2);

        assertThat(rail.cards()).hasSize(2);          // 노출은 자리 수만큼
        assertThat(rail.candidateCount()).isEqualTo(4); // 초과 감지의 근거는 자르기 전 총수
    }

    @Test
    @DisplayName("추천 레일은 featured 플래그만 보고 구독과 무관하다")
    void featuredRail_ignoresSubscription() {
        Company featured = withSubscription("추천", CompanySubscription.freeForUser(TODAY)); // FREE 인데도
        featured.feature();
        withSubscription("유료미추천", CompanySubscription.active(CompanyGrade.ENTERPRISE, TODAY, null)); // 유료인데 추천 아님
        em.flush();

        assertThat(ids(repository.findFeatured(SLOTS))).containsExactly(featured.getId());
    }

    @Test
    @DisplayName("후보가 없으면 빈 레일과 총수 0 을 반환한다")
    void noCandidates_returnsEmptyRail() {
        ShowcaseRail rail = repository.findSpotlight(SLOTS);

        assertThat(rail.cards()).isEmpty();
        assertThat(rail.candidateCount()).isZero();
    }

    // ===== helpers =====

    private List<Long> ids(ShowcaseRail rail) {
        return rail.cards().stream().map(CompanySummary::id).toList();
    }

    // 회사 + 구독 1:1 을 함께 저장한다. 구독은 회사당 반드시 1건 존재하므로(등록 트랜잭션 불변식) 테스트도 같이 만든다.
    private Company withSubscription(String name, CompanySubscription subscription) {
        Company company = Company.createByUser(10L, null, name, "대표자", null,
                Address.of("06236", "서울 강남구", null, "1층"), null, "logo-" + name,
                null, null, null, null, null, null, null, null);
        em.persist(company);
        subscription.assignCompany(company.getId());
        em.persist(subscription);
        return company;
    }
}
