package project.plantly.global.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * {@code company.business_number} 활성 행 부분 유니크 인덱스 생성기.
 *
 * <p>soft delete 하에서 삭제된 회사의 사업자번호를 재사용할 수 있어야 하므로, 전체 UNIQUE(삭제 행 포함) 대신
 * 활성({@code deleted=false}) 행끼리만 유일한 Postgres 부분 인덱스를 쓴다.
 *
 * <p><b>왜 Flyway 가 아니라 ApplicationRunner 인가</b> — 이 인덱스는 Hibernate {@code @Column(unique)}·
 * {@code @Index} 로 표현할 수 없고(부분 {@code WHERE}), Flyway 로도 만들 수 없다. Flyway 는 Hibernate 보다
 * 먼저 실행되는데 {@code company} 테이블은 ddl-auto 가 나중에 만들기 때문이다(Flyway 는 FK 없는 검색 섬만 소유).
 * 그래서 "JPA 로 표현 불가한 인덱스는 별도 소유"라는 기존 원칙(GIN trgm 인덱스와 동일)은 지키되, 시점만
 * 컨텍스트 준비 후(ddl-auto 완료 뒤)로 옮긴다. {@link ReferenceDataSeeder} 와 같은 메커니즘.
 *
 * <p>Postgres 전용 문법이라 실제 DB 가 Postgres 일 때만 실행한다(H2 단위 테스트는 건너뛴다 — 그쪽 유일성은
 * 앱 레벨 사전검증 {@code existsByBusinessNumberAndDeletedFalse} 가 담당). {@code IF NOT EXISTS} 로 멱등하다.
 */
@Component
public class CompanyBusinessNumberIndexInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    public CompanyBusinessNumberIndexInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("postgresql")) {
                return; // H2 등 비-Postgres: 부분 인덱스 미지원 → 앱 레벨 사전검증에 위임
            }
            try (Statement st = conn.createStatement()) {
                // 예전 unique=true 매핑이 만든 평범한 UNIQUE 제약이 남아 있으면 제거(이름 자동생성 → 동적 탐색).
                st.execute("""
                        DO $$
                        DECLARE con text;
                        BEGIN
                          SELECT c.conname INTO con
                            FROM pg_constraint c
                           WHERE c.conrelid = 'company'::regclass
                             AND c.contype = 'u'
                             AND c.conkey = ARRAY[(SELECT a.attnum FROM pg_attribute a
                                                    WHERE a.attrelid = 'company'::regclass
                                                      AND a.attname = 'business_number')];
                          IF con IS NOT NULL THEN
                            EXECUTE 'ALTER TABLE company DROP CONSTRAINT ' || quote_ident(con);
                          END IF;
                        END $$;
                        """);
                // 활성 행끼리만 유일. 삭제된 회사(deleted=true)의 사업자번호는 인덱스에서 빠져 재사용 가능.
                st.execute(
                        "CREATE UNIQUE INDEX IF NOT EXISTS ux_company_business_number_active "
                                + "ON company (business_number) WHERE deleted = false");
            }
        }
    }
}
