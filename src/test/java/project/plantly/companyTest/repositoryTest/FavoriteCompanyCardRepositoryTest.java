package project.plantly.companyTest.repositoryTest;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.companyTest.support.PostgresContainerTest;
import project.plantly.domain.company.entity.Address;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.repository.FavoriteCompanyCardRepository;
import project.plantly.domain.company.search.dto.CompanySummary;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("FavoriteCompanyCardRepository: 내 즐겨찾기 회사 목록 카드")
class FavoriteCompanyCardRepositoryTest extends PostgresContainerTest {

    @Autowired EntityManager em;
    @Autowired FavoriteCompanyCardRepository repository;

    private static final long ME = 100L;
    private static final long OTHER = 200L;

    private static final LocalDateTime EARLIER = LocalDateTime.of(2026, 1, 1, 10, 0);
    private static final LocalDateTime LATER = LocalDateTime.of(2026, 1, 2, 10, 0);

    @Test
    @DisplayName("정렬 키는 회사 등록 시각이 아니라 즐겨찾기한 시각이다 (나중에 담은 회사가 먼저 온다)")
    void ordersByFavoritedAtNotCompanyCreatedAt() {
        // 회사는 first → second 순으로 등록하고(= created_at·id 오름차순),
        Company first = persistCompany("먼저등록된회사");
        Company second = persistCompany("나중등록된회사");
        em.flush();

        // 즐겨찾기는 그 반대로 — second 를 먼저, first 를 나중에 담는다.
        persistFavorite(ME, second.getId(), EARLIER);
        persistFavorite(ME, first.getId(), LATER);

        Page<CompanySummary> page = repository.findFavoritedBy(ME, PageRequest.of(0, 20));

        // 가장 최근에 담은 first 가 맨 위. 회사 등록순(created_at/id DESC)이었다면 second 가 먼저였을 것이다.
        assertThat(page.getContent().stream().map(CompanySummary::id).toList())
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    @DisplayName("내가 담은 미삭제 회사만 반환한다 (삭제된 회사·타인의 즐겨찾기 제외)")
    void returnsOnlyMyNonDeletedFavorites() {
        Company mine = persistCompany("내가담은회사");
        Company deleted = persistCompany("담아뒀는데삭제된회사");
        Company others = persistCompany("남이담은회사");
        deleted.delete();
        em.flush(); // JPA 변경분을 DB 로 내려 JdbcTemplate 가 같은 트랜잭션에서 보게 한다.

        persistFavorite(ME, mine.getId(), EARLIER);
        persistFavorite(ME, deleted.getId(), LATER);   // 회사가 삭제됐으므로 목록에서 빠진다
        persistFavorite(OTHER, others.getId(), LATER); // 타인의 즐겨찾기

        Page<CompanySummary> page = repository.findFavoritedBy(ME, PageRequest.of(0, 20));

        assertThat(page.getContent().stream().map(CompanySummary::id).toList())
                .containsExactly(mine.getId());
        assertThat(page.getTotalElements()).isEqualTo(1); // 총 건수도 삭제 회사를 세지 않는다
    }

    @Test
    @DisplayName("페이지 크기를 넘으면 페이징되고 총 건수는 전체를 센다")
    void paginates() {
        for (int i = 0; i < 3; i++) {
            Company c = persistCompany("회사" + i);
            em.flush(); // company id 확정
            persistFavorite(ME, c.getId(), EARLIER.plusHours(i));
        }

        Page<CompanySummary> firstPage = repository.findFavoritedBy(ME, PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    // ===== helpers =====

    // 소유(company_member)와 무관하게 회사만 저장한다 — 즐겨찾기는 소유가 아니라 뷰어↔회사 관계다.
    private Company persistCompany(String name) {
        Company c = Company.createByUser(OTHER, null, name, "대표자", null,
                Address.of("06236", "서울 강남구", null, "1층"), null, "logo-" + name, null,
                null, null, null, null, null, null, null, null);
        em.persist(c);
        return c;
    }

    // 즐겨찾기 시각을 명시해 심는다. @CreationTimestamp 로 심으면 정렬 검증이 시스템 시계 정밀도에 의존하게 된다
    // (연달아 저장하면 같은 tick 에 걸려 순서가 흔들릴 수 있다) — 정렬 키를 테스트가 직접 통제한다.
    private void persistFavorite(long userId, Long companyId, LocalDateTime favoritedAt) {
        em.createNativeQuery(
                        "insert into company_favorite (user_id, company_id, created_at) values (?1, ?2, ?3)")
                .setParameter(1, userId)
                .setParameter(2, companyId)
                .setParameter(3, favoritedAt)
                .executeUpdate();
    }
}