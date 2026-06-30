package project.plantly.domain.company.search;

import org.springframework.jdbc.core.RowMapper;
import project.plantly.domain.company.search.dto.CompanySummary;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 요약 카드({@link CompanySummary}) 프로젝션 SQL 과 RowMapper. 공개 검색({@link PostgresTrigramCompanySearch})과
 * 내 회사 목록 등 '카드 형태로 회사를 나열'하는 읽기 경로가 공통으로 재사용한다.
 *
 * <p>{@link #CARD_COLUMNS} 는 별칭 {@code c = company} 를 전제로 한 SELECT 컬럼 목록이다(SELECT/FROM 은 호출부가 붙인다).
 * 회사 스칼라 + 회사가 연결한 카테고리/태그/산업군 이름을 회사당 array_agg 로 집계한다. 카테고리는 직접 링크
 * (company_category)만 — closure 조상은 제외. 셋 다 회사가 등록 시 고른 순서(링크/태그의 display_order)로 정렬한다.
 */
public final class CompanyCardSql {

    private CompanyCardSql() {
    }

    public static final String CARD_COLUMNS = """
            c.id, c.company_name, c.intro_title, c.logo_url, c.address,
            c.verified, c.featured, c.spotlight,
            (SELECT array_agg(cat.category_name ORDER BY cc.display_order)
               FROM company_category cc JOIN category cat ON cat.id = cc.category_id
               WHERE cc.company_id = c.id) AS category_names,
            (SELECT array_agg(t.tag_name ORDER BY t.display_order)
               FROM company_tag t WHERE t.company_id = c.id) AS tag_names,
            (SELECT array_agg(ind.industry_name ORDER BY ci.display_order)
               FROM company_industry ci JOIN industry ind ON ind.id = ci.industry_id
               WHERE ci.company_id = c.id) AS industry_names
            """;

    public static final RowMapper<CompanySummary> ROW_MAPPER = (rs, i) -> new CompanySummary(
            rs.getLong("id"),
            rs.getString("company_name"),
            rs.getString("intro_title"),
            rs.getString("logo_url"),
            rs.getString("address"),
            rs.getBoolean("verified"),
            rs.getBoolean("featured"),
            rs.getBoolean("spotlight"),
            toList(rs.getArray("category_names")),
            toList(rs.getArray("tag_names")),
            toList(rs.getArray("industry_names")));

    // PG text[] → List<String>. 매칭 행이 없으면 array_agg 는 NULL → 빈 리스트. null 원소는 제거.
    private static List<String> toList(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        String[] values = (String[]) array.getArray();
        return Arrays.stream(values).filter(Objects::nonNull).toList();
    }
}
