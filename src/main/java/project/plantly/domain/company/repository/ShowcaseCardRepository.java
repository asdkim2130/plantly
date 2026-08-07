package project.plantly.domain.company.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import project.plantly.domain.company.search.CompanyCardSql;
import project.plantly.domain.company.search.dto.CompanySummary;

import java.util.ArrayList;
import java.util.List;

/**
 * 메인 화면 노출 레일(스팟라이트 / 추천) 읽기 전용 리포지토리. 검색·내 회사 목록과 동일한 카드 프로젝션
 * ({@link CompanyCardSql})을 재사용하되, 페이징이 아니라 '자리 수(slots)만큼 잘라 오는' 조회다.
 *
 * <p><b>이 클래스가 스팟라이트 노출 여부를 판단하는 유일한 곳이다.</b> 회사에 저장된 플래그를 읽는 게 아니라
 * 조회 시점에 구독을 보고 자격을 파생한다 — 그래서 구독이 만료되면 별도 배치 없이 자동으로 레일에서 빠진다.
 * (등급 혜택을 등록 시점에 company 컬럼으로 굳히면 만료돼도 회수되지 않는다. effectiveGrade 와 같은 철학.)
 *
 * <p>스팟라이트 후보는 두 갈래로 들어온다:
 * <ul>
 *   <li><b>요금제 자격</b> — 유료 활성 구독(PREMIUM/ENTERPRISE, 미만료). 관리자 손이 필요 없다.
 *   <li><b>관리자 고정(pin)</b> — {@code company.spotlight = true}. 요금제 밖에서 노출해야 하는
 *       제휴·이벤트용 통로다.
 * </ul>
 *
 * <p>{@code status = 'ACTIVE'} 한 조건이 두 가지를 동시에 제외한다 — 체험(TRIAL)은 유료 전환 동기를 남기려고,
 * 관리자 등록(ADMIN_EXEMPT)은 명목상 grade 가 ENTERPRISE 라 자격으로 읽으면 관리자가 등록한 모든 회사가
 * 노출되기 때문이다. 관리자 등록 회사를 띄우려면 pin 을 쓴다.
 */
@Repository
public class ShowcaseCardRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ShowcaseCardRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 레일 1개의 조회 결과. {@code cards} 는 자리 수만큼 잘린 노출분이고,
     * {@code candidateCount} 는 자르기 전 후보 총수다 — 자리보다 많아졌는지 판단하는 근거.
     */
    public record ShowcaseRail(List<CompanySummary> cards, int candidateCount) {
    }

    // 두 레일 공통: 삭제되지 않고 공개된 회사만. 공개 검색과 같은 가시성 규칙을 미러한다.
    // 검색과 달리 company_search_document 는 조인하지 않는다 — 색인이 밀렸다는 이유로
    // 유료 고객이 메인에서 사라지면 안 된다.
    private static final String VISIBLE = " c.deleted = false AND c.visibility = 'PUBLIC' ";

    // 자르기 전 후보 총수를 같은 쿼리에서 얻는다(윈도우 함수). 초과 감지 때문에 count 쿼리를 한 번 더
    // 던지지 않기 위한 것으로, LIMIT 이전에 평가되므로 잘린 뒤에도 총수는 그대로다.
    private static final String CANDIDATE_COUNT = ", count(*) OVER () AS candidate_count ";

    private static final String SPOTLIGHT_SQL = """
            SELECT %s %s
              FROM company c
              LEFT JOIN company_subscription s ON s.company_id = c.id
             WHERE %s
               AND (
                     c.spotlight = true
                  OR (s.status = 'ACTIVE'
                      AND s.grade IN ('PREMIUM', 'ENTERPRISE')
                      AND (s.expires_at IS NULL OR s.expires_at >= current_date))
                   )
             ORDER BY c.spotlight DESC,
                      c.spotlight_order ASC,
                      s.started_at DESC NULLS LAST,
                      c.id DESC
             LIMIT :slots
            """.formatted(CompanyCardSql.CARD_COLUMNS, CANDIDATE_COUNT, VISIBLE);

    private static final String FEATURED_SQL = """
            SELECT %s %s
              FROM company c
             WHERE %s
               AND c.featured = true
             ORDER BY c.created_at DESC, c.id DESC
             LIMIT :slots
            """.formatted(CompanyCardSql.CARD_COLUMNS, CANDIDATE_COUNT, VISIBLE);

    /**
     * 스팟라이트 레일. 관리자 고정분이 먼저, 그다음 요금제 자격분이 최근 구독순으로 온다.
     *
     * <p>정렬은 <b>임시 규칙</b>이다 — 후보가 자리보다 많아지면 늦게 계약한 고객이 영영 노출되지 않는다.
     * 후보 > 자리가 실제로 발생하면(호출부가 경고 로그로 감지한다) 이 ORDER BY 를 로테이션으로 교체한다.
     * 날짜를 시드로 쓰는 셔플({@code md5(c.id::text || current_date::text)})이면 노출 이력을 저장하지 않고도
     * 매일 다른 조합이 나오므로, 지금 미리 쌓아둬야 하는 데이터는 없다.
     */
    public ShowcaseRail findSpotlight(int slots) {
        return queryRail(SPOTLIGHT_SQL, slots);
    }

    /**
     * 추천 레일. 등급과 무관한 순수 관리자 큐레이션({@code company.featured})이라 구독을 보지 않는다.
     */
    public ShowcaseRail findFeatured(int slots) {
        return queryRail(FEATURED_SQL, slots);
    }

    private ShowcaseRail queryRail(String sql, int slots) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("slots", slots);

        return jdbc.query(sql, params, rs -> {
            List<CompanySummary> cards = new ArrayList<>();
            int candidateCount = 0;
            int row = 0;
            while (rs.next()) {
                cards.add(CompanyCardSql.ROW_MAPPER.mapRow(rs, row++));
                // 모든 행에 같은 값이 실려 온다. 0 건이면 루프를 안 타므로 초기값 0 이 그대로 맞다.
                candidateCount = rs.getInt("candidate_count");
            }
            return new ShowcaseRail(cards, candidateCount);
        });
    }
}
