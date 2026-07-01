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
 * 내 회사 목록 읽기 전용 리포지토리. 로그인 유저가 소유(company.user_id = 본인)한 미삭제 회사를 요약 카드로 나열한다.
 *
 * <p>공개 검색({@code PostgresTrigramCompanySearch})과 동일한 카드 프로젝션({@link CompanyCardSql})을 재사용하되,
 * 검색 도큐먼트 JOIN·키워드·패싯은 없다 — 소유자 필터 + 미삭제 + 최신 등록순뿐이다. 검색 seam(ES 교체 지점)과
 * 무관한 소유자 스코프 조회라 검색 인터페이스가 아닌 별도 경로로 둔다.
 */
@Repository
public class OwnedCompanyCardRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public OwnedCompanyCardRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String FROM = " FROM company c ";

    // 소유자 본인 + 미삭제. (삭제된 회사는 내 목록에서도 제외 — 소유자 상세조회와 달리 카드 목록은 활성 회사만 노출)
    private static final String WHERE = " WHERE c.user_id = :userId AND c.deleted = false ";

    // 최신 등록순. id 는 안정 페이징용 tie-breaker.
    private static final String ORDER_BY = " ORDER BY c.created_at DESC, c.id DESC ";

    public Page<CompanySummary> findOwnedBy(Long userId, Pageable pageable) {
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
