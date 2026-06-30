package project.plantly.companyTest.repositoryTest;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.companyTest.support.PostgresContainerTest;
import project.plantly.domain.company.category.Category;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanyTag;
import project.plantly.domain.company.entity.link.CompanyCategory;
import project.plantly.domain.company.entity.link.CompanyIndustry;
import project.plantly.domain.company.industry.Industry;
import project.plantly.domain.company.repository.OwnedCompanyCardRepository;
import project.plantly.domain.company.search.dto.CompanySummary;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("OwnedCompanyCardRepository: 내 회사 목록 카드")
class OwnedCompanyCardRepositoryTest extends PostgresContainerTest {

    @Autowired EntityManager em;
    @Autowired OwnedCompanyCardRepository repository;

    private static final long OWNER = 100L;
    private static final long OTHER = 200L;

    @Test
    @DisplayName("소유자 본인의 미삭제 회사만, 최신 등록순으로 카드를 반환한다 (삭제·타인 회사 제외)")
    void returnsOwnedNonDeletedLatestFirst() {
        Company older = persistCompany(OWNER, "오래된내회사");
        Company newer = persistCompany(OWNER, "최신내회사");
        Company deleted = persistCompany(OWNER, "내삭제회사");
        deleted.delete();
        persistCompany(OTHER, "남의회사"); // 타인 소유 → 제외
        em.flush(); // JPA 변경분을 DB 로 내려 JdbcTemplate 가 같은 트랜잭션에서 보게 한다.

        Page<CompanySummary> page = repository.findOwnedBy(OWNER, PageRequest.of(0, 20));

        // 최신순(created_at DESC, id DESC) — 삭제·타인 회사는 빠진다.
        assertThat(page.getContent().stream().map(CompanySummary::id).toList())
                .containsExactly(newer.getId(), older.getId());
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("카드에 회사가 연결한 카테고리/태그/산업군 이름을 선택 순서로 담는다 (closure 조상 제외)")
    void cardAggregatesNames() {
        Company a = persistCompany(OWNER, "가가");
        Category root = Category.createRoot("제조", "MFG", null, null, 0);
        em.persist(root);
        Category precision = Category.createChild(root, "정밀가공", "MFG-P", null, null, 0);
        em.persist(precision);
        Industry industry = Industry.create("농업기술", "AGRI", null, null, 0);
        em.persist(industry);
        em.persist(new CompanyCategory(a, precision, 0)); // 직접 링크 = 중분류만 (대분류 제조는 closure 에만)
        em.persist(new CompanyIndustry(a, industry, 0));
        em.persist(new CompanyTag(a, "스마트팜", 0));
        em.persist(new CompanyTag(a, "IoT", 1));
        em.flush();

        CompanySummary card = repository.findOwnedBy(OWNER, PageRequest.of(0, 20)).getContent().get(0);

        assertThat(card.categoryNames()).containsExactly("정밀가공"); // 직접 링크만, closure 조상(제조) 제외
        assertThat(card.tagNames()).containsExactly("스마트팜", "IoT"); // display_order 순
        assertThat(card.industryNames()).containsExactly("농업기술");
    }

    @Test
    @DisplayName("페이지 크기를 넘으면 페이징되고 총 건수는 전체를 센다")
    void paginates() {
        for (int i = 0; i < 3; i++) {
            persistCompany(OWNER, "회사" + i);
        }
        em.flush();

        Page<CompanySummary> firstPage = repository.findOwnedBy(OWNER, PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    // ===== helpers =====

    private Company persistCompany(Long ownerId, String name) {
        Company c = Company.createByUser(ownerId, null, name, "대표자", null,
                "06236", "서울 강남구", "1층", null, "logo-" + name,
                null, null, null, null, null, null, null, null);
        em.persist(c);
        return c;
    }
}
