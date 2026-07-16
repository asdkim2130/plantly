package project.plantly.companyTest.repositoryTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import project.plantly.domain.company.dto.OwnerSubscriptionRow;
import project.plantly.domain.company.entity.Address;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.entity.link.CompanyMember;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.enums.MemberRole;
import project.plantly.domain.company.repository.CompanySubscriptionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// 소유 회사 구독 배치 조회(관리자 유저 목록 등급 배지용) JPQL 검증 — 세타 조인 + userId IN + role 필터가 실제로 도는지.
// (H2 로 충분: pg 전용 문법 없음. effectiveGrade 파생은 엔티티/관리자카드 테스트가 담당하고, 여기선 짝 매핑·필터를 본다)
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CompanySubscriptionRepository: 소유 회사 구독 배치 조회")
class CompanySubscriptionRepositoryTest {

    @Autowired
    private CompanySubscriptionRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("여러 유저의 소유(OWNER) 회사 구독을 (userId, 구독) 짝으로 조회하고, 소유 회사가 없는 유저는 제외한다")
    void findOwnerSubscriptionRows_pairsOwnerWithSubscription() {
        long ownerA = 10L, ownerB = 20L, noCompany = 30L;
        long companyA = seedOwnedCompanyWithSubscription(ownerA, CompanyGrade.PREMIUM);
        long companyB = seedOwnedCompanyWithSubscription(ownerB, CompanyGrade.STANDARD);
        em.flush();

        List<OwnerSubscriptionRow> rows = repository.findOwnerSubscriptionRows(
                MemberRole.OWNER, List.of(ownerA, ownerB, noCompany));

        Map<Long, OwnerSubscriptionRow> byUser =
                rows.stream().collect(Collectors.toMap(OwnerSubscriptionRow::userId, r -> r));
        assertThat(byUser.keySet()).containsExactlyInAnyOrder(ownerA, ownerB); // 무소유 유저(noCompany) 제외
        assertThat(byUser.get(ownerA).subscription().getCompanyId()).isEqualTo(companyA);
        assertThat(byUser.get(ownerA).subscription().getGrade()).isEqualTo(CompanyGrade.PREMIUM);
        assertThat(byUser.get(ownerB).subscription().getCompanyId()).isEqualTo(companyB);
        assertThat(byUser.get(ownerB).subscription().getGrade()).isEqualTo(CompanyGrade.STANDARD);
    }

    // 회사 + OWNER 멤버 + 활성 구독을 저장하고 회사 id 를 반환한다.
    private long seedOwnedCompanyWithSubscription(Long ownerId, CompanyGrade grade) {
        Company company = Company.createByUser(ownerId, null, "회사-" + ownerId, "대표", null,
                Address.of("06236", "서울", null, "1층"), null, "logo", null, null, null, null, null, null, null, null);
        em.persist(company);
        em.persist(CompanyMember.owner(company.getId(), ownerId));
        CompanySubscription subscription = CompanySubscription.active(grade, LocalDate.now(), null);
        subscription.assignCompany(company.getId());
        em.persist(subscription);
        return company.getId();
    }
}
