package project.plantly.global.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 참조데이터(행정구역/국가) 시드.
 *
 * <p>예전엔 {@code spring.sql.init} + {@code spring.jpa.defer-datasource-initialization=true} 로
 * Hibernate 가 스키마를 만든 "뒤"에 seed 를 실행했다. 그러나 Flyway 를 도입하면 defer 가
 * {@code flyway <-> entityManagerFactory} 순환 의존(Spring Boot 제약)을 유발한다.
 *
 * <p>그래서 seed 를 그 메커니즘에서 떼어내, 컨텍스트가 완전히 준비된 뒤(Flyway·Hibernate 모두 끝남)
 * {@link ApplicationRunner} 로 직접 실행한다. seed 는 {@code ON CONFLICT DO NOTHING} 으로 멱등이라
 * 매 기동 안전하다. Postgres 전용 문법이므로 H2 를 쓰는 test 프로파일에서는 제외한다.
 */
@Component
@Profile("!test")
public class ReferenceDataSeeder implements ApplicationRunner {

    private final DataSource dataSource;

    public ReferenceDataSeeder(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/seed/domestic-region.sql"),
                new ClassPathResource("db/seed/country.sql")
        );
        populator.execute(dataSource);
    }
}
