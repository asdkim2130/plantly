package project.plantly.domain.company.search;

import org.springframework.jdbc.core.RowMapper;
import project.plantly.domain.company.enums.RegistrationSource;
import project.plantly.domain.company.search.dto.AdminCompanySummary;

import java.time.LocalDateTime;

/**
 * 관리자 회사 목록 카드({@link AdminCompanySummary}) 프로젝션 SQL·RowMapper.
 *
 * <p>공개 카드 컬럼({@link CompanyCardSql#CARD_COLUMNS})을 그대로 이어붙이고 운영 컬럼(deleted/user_id/
 * registration_source/created_at)만 추가한다 — 카테고리/태그/산업군 array_agg 집계는 복붙 없이 공유한다.
 * 별칭 {@code c = company} 전제(SELECT/FROM 은 호출부가 붙인다).
 */
public final class AdminCompanyCardSql {

    private AdminCompanyCardSql() {
    }

    public static final String COLUMNS = CompanyCardSql.CARD_COLUMNS
            + ", c.deleted, c.user_id, c.registration_source, c.created_at";

    public static final RowMapper<AdminCompanySummary> ROW_MAPPER = (rs, i) -> new AdminCompanySummary(
            rs.getLong("id"),
            rs.getString("company_name"),
            rs.getString("intro_title"),
            rs.getString("logo_url"),
            rs.getString("address"),
            rs.getBoolean("verified"),
            rs.getBoolean("featured"),
            rs.getBoolean("spotlight"),
            rs.getBoolean("deleted"),
            rs.getObject("user_id", Long.class),                 // 미연동이면 null (getLong 은 0 으로 뭉갬)
            RegistrationSource.valueOf(rs.getString("registration_source")),
            rs.getObject("created_at", LocalDateTime.class),
            CompanyCardSql.toList(rs.getArray("category_names")),
            CompanyCardSql.toList(rs.getArray("tag_names")),
            CompanyCardSql.toList(rs.getArray("industry_names")));
}
