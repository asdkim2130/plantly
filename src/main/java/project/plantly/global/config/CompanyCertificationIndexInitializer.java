package project.plantly.global.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * {@code company_certification} 유일성 인덱스 생성기.
 *
 * <p>인증 링크의 유일성은 다른 링크(카테고리·국가·지역·산업)와 달리 한 갈래가 아니다.
 * <ul>
 *   <li><b>일반 인증</b> — 회사당 마스터 1건. {@code (company_id, certification_id)}
 *   <li><b>기타(ETC)</b> — 마스터 목록에 없는 인증을 회사가 직접 적어 넣는 앵커라, 같은 마스터에 이름만 다른
 *       링크가 여러 건 붙는다. {@code (company_id, certification_id, custom_name)}
 * </ul>
 *
 * <p>그래서 하나의 전체 UNIQUE 로는 표현할 수 없다. 세 컬럼 UNIQUE 하나로 합치는 것도 안 된다 —
 * Postgres 의 UNIQUE 는 NULL 을 서로 다른 값으로 취급해서(NULLS DISTINCT 가 기본), 일반 인증
 * ({@code custom_name IS NULL})의 중복이 통째로 빠져나간다. {@code custom_name} 의 NULL 여부로 갈리는
 * <b>부분</b> 유니크 인덱스 두 개가 정확한 표현이다.
 *
 * <p><b>왜 Flyway 가 아니라 ApplicationRunner 인가</b> — {@link CompanyBusinessNumberIndexInitializer} 와
 * 같은 이유다. 부분 인덱스는 JPA 로 표현할 수 없고, Flyway 는 ddl-auto 보다 먼저 돌아 엔티티 테이블을 아직 못 본다.
 *
 * <p>Postgres 일 때만 실행한다(H2 슬라이스 테스트는 건너뛴다). 그쪽에서 중복이 안 생기는 건 쓰기 경로가
 * 요청을 (id, custom_name) 기준으로 중복 제거하고, 교체(PUT)는 전량 삭제 후 재삽입이기 때문이다.
 *
 * <p>"custom_name 이 있으면 마스터는 ETC, 없으면 ETC 가 아니다"는 짝 규칙은 여기서 강제하지 않는다 —
 * 마스터의 type 을 봐야 하는 교차 테이블 조건이라 CHECK 로 쓰려면 링크에 type 을 비정규화해야 한다.
 * 그 규칙은 마스터를 손에 쥔 {@code CompanyCertification} 생성자가 소유한다.
 */
@Component
@Order(21)
public class CompanyCertificationIndexInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    public CompanyCertificationIndexInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("postgresql")) {
                return; // H2 등 비-Postgres: 부분 인덱스 미지원 → 쓰기 경로의 중복 제거에 위임
            }
            try (Statement st = conn.createStatement()) {
                // 예전 @UniqueConstraint(company_id, certification_id) 가 만든 UNIQUE 제약 제거.
                // ddl-auto 는 제약을 지우지 않으므로 남겨두면 기타 인증 2건째부터 저장이 막힌다(이름이 달라도).
                // 이름이 자동생성이라 컬럼 조합으로 찾는다.
                st.execute("""
                        DO $$
                        DECLARE con text;
                        BEGIN
                          IF to_regclass('company_certification') IS NULL THEN
                            RETURN;
                          END IF;
                          SELECT c.conname INTO con
                            FROM pg_constraint c
                           WHERE c.conrelid = 'company_certification'::regclass
                             AND c.contype = 'u'
                             AND c.conkey @> ARRAY[(SELECT a.attnum FROM pg_attribute a
                                                     WHERE a.attrelid = 'company_certification'::regclass
                                                       AND a.attname = 'certification_id')]
                             AND c.conkey @> ARRAY[(SELECT a.attnum FROM pg_attribute a
                                                     WHERE a.attrelid = 'company_certification'::regclass
                                                       AND a.attname = 'company_id')];
                          IF con IS NOT NULL THEN
                            EXECUTE 'ALTER TABLE company_certification DROP CONSTRAINT ' || quote_ident(con);
                          END IF;
                        END $$;
                        """);
                // 일반 인증: 회사당 마스터 1건.
                st.execute("""
                        CREATE UNIQUE INDEX IF NOT EXISTS ux_company_certification_master
                            ON company_certification (company_id, certification_id)
                         WHERE custom_name IS NULL
                        """);
                // 기타 인증: 같은 마스터라도 직접 입력한 이름이 다르면 별개. 같은 이름은 1건.
                st.execute("""
                        CREATE UNIQUE INDEX IF NOT EXISTS ux_company_certification_custom
                            ON company_certification (company_id, certification_id, custom_name)
                         WHERE custom_name IS NOT NULL
                        """);
            }
        }
    }
}
