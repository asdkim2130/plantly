package project.plantly.domain.company.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import project.plantly.domain.company.search.CompanyCardSql;
import project.plantly.domain.company.search.dto.CompanySummary;

import java.util.List;

/**
 * 내 즐겨찾기 회사 목록 읽기 전용 리포지토리. 로그인 유저가 즐겨찾기한 미삭제 회사를 요약 카드로 나열한다.
 *
 * <p>공개 검색·내 회사 목록과 동일한 카드 프로젝션({@link CompanyCardSql})을 재사용하되, 검색 도큐먼트 JOIN·
 * 키워드·패싯은 없다 — 즐겨찾기 소유자 필터 + 미삭제 + 즐겨찾기순뿐이다. 즐겨찾기는 회사의 속성이 아니라
 * 뷰어↔회사 관계이고 색인 대상이 아니므로, 검색 seam({@code CompanySearchRepository}) 밖의 별도 경로로 둔다.
 * ({@link OwnedCompanyCardRepository} 와 같은 구조 — 필터 조건과 정렬 키만 다르다.)
 */
@Repository
public class FavoriteCompanyCardRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public FavoriteCompanyCardRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // unique(user_id, company_id) 라 JOIN 이 회사당 1행을 넘지 않는다(중복 카드 없음).
    private static final String FROM = """
             FROM company c
             JOIN company_favorite f ON f.company_id = c.id
            """;

    // 내 즐겨찾기 + 미삭제 + 공개. 즐겨찾기해 둔 회사가 소프트 삭제되거나 비공개로 전환되면 목록에서 빠지고,
    // 다시 공개되면 돌아온다(공개 조회와 동일 정책). 즐겨찾기 원천 행은 유지되므로 재공개 시 그대로 노출된다.
    private static final String WHERE =
            " WHERE f.user_id = :userId AND c.deleted = false AND c.visibility = 'PUBLIC' ";

    // 즐겨찾기순(내가 담은 최신 순). 회사 생성일이 아니라 즐겨찾기 시각 기준이며,
    // 검색의 spotlight/featured 우선순위는 개입하지 않는다 — 내 목록의 순서는 내가 담은 순서다.
    // id 는 안정 페이징용 tie-breaker.
    private static final String ORDER_BY = " ORDER BY f.created_at DESC, c.id DESC ";

    public Page<CompanySummary> findFavoritedBy(Long userId, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("limit", pageable.getPageSize())
                .addValue("offset", pageable.getOffset());

        List<CompanySummary> content = jdbc.query(
                "SELECT " + CompanyCardSql.CARD_COLUMNS + FROM + WHERE + ORDER_BY + " LIMIT :limit OFFSET :offset",
                params, CompanyCardSql.ROW_MAPPER);

        return PageableExecutionUtils.getPage(content, pageable, () -> {
            Long total = jdbc.queryForObject("SELECT count(*)" + FROM + WHERE, params, Long.class);
            return total == null ? 0L : total;
        });
    }
}
