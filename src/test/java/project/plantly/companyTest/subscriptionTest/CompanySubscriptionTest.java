package project.plantly.companyTest.subscriptionTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.enums.SubscriptionStatus;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// CompanySubscription: 정책이 참조하는 '유효 등급(effectiveGrade)' 파생 로직을 못 박는다.
// 저장하는 건 grade/status/기간이라는 사실뿐이고, 유효 등급은 asOf 기준으로 계산된다.
@DisplayName("CompanySubscription: 유효 등급 파생")
class CompanySubscriptionTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate EXPIRES = LocalDate.of(2026, 6, 30);
    private static final LocalDate BEFORE_EXPIRY = LocalDate.of(2026, 3, 1);
    private static final LocalDate AFTER_EXPIRY = LocalDate.of(2026, 12, 1);

    @Test
    @DisplayName("FREE 무기한: 언제 조회해도 FREE, 면제 아님, 만료 없음")
    void freeForUser_neverExpires() {
        CompanySubscription sub = CompanySubscription.freeForUser(START);

        assertThat(sub.getGrade()).isEqualTo(CompanyGrade.FREE);
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.isExempt()).isFalse();
        assertThat(sub.isExpired(AFTER_EXPIRY)).isFalse();
        assertThat(sub.effectiveGrade(AFTER_EXPIRY)).isEqualTo(CompanyGrade.FREE);
    }

    @Test
    @DisplayName("활성 유료구독: 만료 전이면 산 등급 그대로")
    void active_beforeExpiry_keepsGrade() {
        CompanySubscription sub = CompanySubscription.active(CompanyGrade.ENTERPRISE, START, EXPIRES);

        assertThat(sub.isExpired(BEFORE_EXPIRY)).isFalse();
        assertThat(sub.effectiveGrade(BEFORE_EXPIRY)).isEqualTo(CompanyGrade.ENTERPRISE);
    }

    @Test
    @DisplayName("활성 유료구독: 연장 안 하면 만료일 이후 자동으로 FREE 로 강등")
    void active_afterExpiry_downgradesToFree() {
        CompanySubscription sub = CompanySubscription.active(CompanyGrade.ENTERPRISE, START, EXPIRES);

        assertThat(sub.isExpired(AFTER_EXPIRY)).isTrue();
        assertThat(sub.effectiveGrade(AFTER_EXPIRY)).isEqualTo(CompanyGrade.FREE);
        // 저장된 등급은 이력으로 남는다(계산만 강등).
        assertThat(sub.getGrade()).isEqualTo(CompanyGrade.ENTERPRISE);
    }

    @Test
    @DisplayName("체험: 만료 전엔 혜택 등급, 만료 후엔 FREE")
    void trial_expiryBehavesLikeActive() {
        CompanySubscription sub = CompanySubscription.trial(CompanyGrade.PREMIUM, START, EXPIRES);

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.TRIAL);
        assertThat(sub.effectiveGrade(BEFORE_EXPIRY)).isEqualTo(CompanyGrade.PREMIUM);
        assertThat(sub.effectiveGrade(AFTER_EXPIRY)).isEqualTo(CompanyGrade.FREE);
    }

    @Test
    @DisplayName("관리자 면제: 면제 플래그가 서고, 만료 개념 없이 저장 등급 유지")
    void adminExempt_isExemptAndKeepsGrade() {
        CompanySubscription sub = CompanySubscription.adminExempt(START);

        assertThat(sub.isExempt()).isTrue();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ADMIN_EXEMPT);
        assertThat(sub.effectiveGrade(AFTER_EXPIRY)).isEqualTo(CompanyGrade.ENTERPRISE);
    }
}
