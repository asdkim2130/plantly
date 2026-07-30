package project.plantly.domain.company.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import project.plantly.domain.company.search.dto.CompanySummary;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link CompanySearchRepository} 의 Postgres 구현. pg_trgm 비정규화 도큐먼트 위에서 네이티브 SQL 로
 * 통합/고급 텍스트 매칭 + 패싯 필터 + 기본 정렬 + 페이징을 수행한다. (ES 전환 시 이 구현만 교체.)
 *
 * <p>텍스트 조건은 도큐먼트({@code company_search_document}), 카드·정렬 필드는 {@code company} 본체에서
 * 가져오므로 둘을 PK 로 JOIN 한다. 도큐먼트가 있는(=writer 가 색인한) 회사만, 그리고 삭제되지 않은 회사만 노출.
 * 통합/고급 매칭은 모두 {@code ILIKE '%term%'}(대소문자 무시 부분일치, GIN trgm 가속)이며 term 의 LIKE
 * 메타문자(%, _)는 이스케이프한다. 패싯은 차원 내부는 OR(IN), 차원 간은 AND.
 * 단 인증만 예외로 type 내부 OR + type 간 AND ({@link #appendCertificationFacet}).
 */
@Repository
public class PostgresTrigramCompanySearch implements CompanySearchRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PostgresTrigramCompanySearch(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String FROM = """
            FROM company c
            JOIN company_search_document d ON d.company_id = c.id
            """;

    // 카드 컬럼 + FROM(도큐먼트 JOIN). 카드 프로젝션·RowMapper 는 내 회사 목록과 공유(CompanyCardSql).
    private static final String SELECT_CARD = "SELECT " + CompanyCardSql.CARD_COLUMNS + FROM;

    // 기본 정렬: 스팟라이트 → 추천 → 최신. id 는 안정 페이징용 tie-breaker.
    private static final String ORDER_BY =
            " ORDER BY c.spotlight DESC, c.featured DESC, c.created_at DESC, c.id DESC";

    @Override
    public Page<CompanySummary> search(CompanySearchCriteria criteria, Pageable pageable) {
        List<String> conditions = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        conditions.add("c.deleted = false");
        conditions.add("c.visibility = 'PUBLIC'");   // 비공개 회사는 공개 목록/검색에서 제외
        appendKeyword(criteria.keyword(), conditions, params);
        appendAdvanced(criteria.advanced(), conditions, params);
        // 인증은 type(경영시스템/산업특화/시장진입) 내부 OR + type 간 AND 이라 전용 처리.
        appendCertificationFacet(conditions, params, criteria.certificationIds());
        appendFacet(conditions, params, "industryIds", criteria.industryIds(), "company_industry", "industry_id");
        // 카테고리는 closure 멤버십으로 선택 id + 후손 서브트리를 한 번에 매칭.
        appendFacet(conditions, params, "categoryIds", criteria.categoryIds(), "company_category_closure", "category_id");

        String where = " WHERE " + String.join(" AND ", conditions);

        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<CompanySummary> content = jdbc.query(
                SELECT_CARD + where + ORDER_BY + " LIMIT :limit OFFSET :offset",
                params, CompanyCardSql.ROW_MAPPER);

        return PageableExecutionUtils.getPage(content, pageable, () -> {
            Long total = jdbc.queryForObject("SELECT count(*) " + FROM + where, params, Long.class);
            return total == null ? 0L : total;
        });
    }

    // 통합검색: 공백으로 나눈 각 토큰을 search_all 에 AND 로 부분일치(서로 다른 필드에 흩어져 있어도 매칭).
    private void appendKeyword(String keyword, List<String> conditions, MapSqlParameterSource params) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        String[] tokens = keyword.trim().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            String name = "kw" + i;
            conditions.add("d.search_all ILIKE :" + name + " ESCAPE '\\'");
            params.addValue(name, LikePatterns.contains(tokens[i]));
        }
    }

    // 고급검색: 지정된 필드만 해당 도큐먼트 컬럼에 부분일치.
    private void appendAdvanced(CompanySearchCriteria.AdvancedText adv, List<String> conditions, MapSqlParameterSource params) {
        if (adv == null) {
            return;
        }
        appendLike(conditions, params, "advName", "d.company_name", adv.companyName());
        appendLike(conditions, params, "advIntro", "d.intro_title", adv.introTitle());
        appendLike(conditions, params, "advContent", "d.content", adv.content());
        appendLike(conditions, params, "advCeo", "d.ceo_name", adv.ceoName());
        appendLike(conditions, params, "advAddr", "d.address", adv.address());
        appendLike(conditions, params, "advDetail", "d.detail_address", adv.detailAddress());
        appendLike(conditions, params, "advRef", "d.reference_text", adv.reference());
        appendLike(conditions, params, "advEquip", "d.equipment_text", adv.equipment());
        appendLike(conditions, params, "advMat", "d.material_text", adv.material());
    }

    private void appendLike(List<String> conditions, MapSqlParameterSource params, String name, String column, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        conditions.add(column + " ILIKE :" + name + " ESCAPE '\\'");
        params.addValue(name, LikePatterns.contains(value));
    }

    // 패싯: 선택 id 중 하나라도 링크된 회사(차원 내부 OR). 차원이 여러 개면 호출마다 AND 로 누적된다.
    private void appendFacet(List<String> conditions, MapSqlParameterSource params,
                             String name, List<Long> ids, String linkTable, String idColumn) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        conditions.add("c.id IN (SELECT company_id FROM " + linkTable + " WHERE " + idColumn + " IN (:" + name + "))");
        params.addValue(name, ids);
    }

    /**
     * 인증 패싯: 같은 type 안에서는 OR, 다른 type 사이에서는 AND.
     *
     * <p>프론트가 type 별 드롭다운을 분리해 보여주므로 각 드롭다운은 독립 조건으로 읽힌다.
     * 예) 경영시스템에서 ISO 9001·14001, 시장진입에서 KC 를 골랐다면 {@code (9001 or 14001) and KC}.
     *
     * <p>구현: 선택 id 들이 걸쳐 있는 type 이 n 개면 회사는 그 n 개 type 을 전부 커버해야 한다.
     * 한 type 안에서는 선택 id 중 하나만 보유해도 그 type 이 커버되므로 type 내 OR 이 자동으로 성립한다.
     * 기대 type 수를 SQL 안에서 세므로 호출부는 id → type 매핑을 알 필요가 없고, 쿼리 파라미터도
     * 평면 {@code certificationIds} 를 그대로 쓴다(API 계약 불변).
     *
     * <p>존재하지 않는 id 만 넘어오면 양변이 모두 0 건이 되어 결과가 비는데, "그 인증을 가진 회사가 없다"와
     * 같은 뜻이라 의도된 동작이다.
     */
    private void appendCertificationFacet(List<String> conditions, MapSqlParameterSource params, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        conditions.add("""
                c.id IN (SELECT cc.company_id
                           FROM company_certification cc
                           JOIN certification ce ON ce.id = cc.certification_id
                          WHERE cc.certification_id IN (:certIds)
                          GROUP BY cc.company_id
                         HAVING count(DISTINCT ce.type)
                                = (SELECT count(DISTINCT type) FROM certification WHERE id IN (:certIds)))""");
        params.addValue("certIds", ids);
    }
}
