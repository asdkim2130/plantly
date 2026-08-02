package project.plantly.global.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 시드 리셋. 회사 도메인·계정·검색 섬을 비우고 id 시퀀스를 1부터 다시 시작한다.
 *
 * <p><b>보존 목록(preserve-list) 방식인 이유</b> — 지울 테이블을 나열하면 새 엔티티가 생길 때마다 여기에
 * 추가하는 걸 잊어 유령 행이 남는다. 반대로 "이것만 남기고 전부"로 뒤집으면, 빠뜨렸을 때의 결과가
 * "안 지워짐"이 아니라 "지워짐"이라 시드 직후 바로 드러난다. 마스터 시드는 {@code ReferenceDataSeeder}
 * 가 매 기동 멱등으로 다시 넣으므로 보존이 필수는 아니지만, 쓸데없이 재적재하지 않도록 남긴다.
 *
 * <p>검색 섬({@code company_search_document} / {@code company_category_closure})은 FK 가 없어서
 * 회사만 지우면 고아 행이 남고, 그대로 두면 존재하지 않는 회사가 검색 결과에 뜬다. 보존 목록에 없으므로
 * 자동으로 함께 비워진다 — 이 방식을 택한 실질적인 이유이기도 하다.
 */
@Slf4j
@Component
@Profile(SeedConfig.PROFILE)
@RequiredArgsConstructor
public class SeedResetter {

    // 지우지 않는 테이블. 마스터 5종 + Flyway 이력.
    private static final Set<String> PRESERVED = Set.of(
            "flyway_schema_history",
            "category",
            "industry",
            "certification",
            "country",
            "domestic_region"
    );

    private final JdbcTemplate jdbcTemplate;

    public void reset() {
        List<String> targets = jdbcTemplate.queryForList("""
                        SELECT table_name FROM information_schema.tables
                         WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                         ORDER BY table_name
                        """, String.class)
                .stream()
                .filter(name -> !PRESERVED.contains(name))
                .toList();

        if (targets.isEmpty()) {
            log.info("[seed] 리셋 대상 테이블이 없습니다.");
            return;
        }

        // 한 문장으로 묶어야 서로를 참조하는 테이블이 순서와 무관하게 함께 비워진다.
        // RESTART IDENTITY 로 id 를 1부터 다시 매겨, 같은 순서로 심으면 케이스별 id 가 재현된다.
        String quoted = targets.stream().map(name -> "\"" + name + "\"").reduce((a, b) -> a + ", " + b).orElseThrow();
        jdbcTemplate.execute("TRUNCATE TABLE " + quoted + " RESTART IDENTITY CASCADE");

        log.info("[seed] {}개 테이블을 비웠습니다: {}", targets.size(), String.join(", ", targets));
    }
}
