package project.plantly.companyTest.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 검색(pg_trgm/GIN) 통합 테스트 베이스. test 프로파일은 H2 라 trgm·string_agg·ON CONFLICT 검증이
 * 불가능하므로, 일회용 Postgres 컨테이너로 datasource 를 덮어쓰고 Flyway 를 켜서 검색 섬(V1)을 만든다.
 * 엔티티 테이블은 그대로 ddl-auto(create-drop)가 만든다(Flyway 가 Hibernate 보다 먼저 실행).
 *
 * <p>싱글톤 컨테이너 패턴: static 블록에서 한 번 start 하고 멈추지 않는다(Ryuk 이 JVM 종료 시 정리).
 * {@code @Container} 로 클래스 종료 시 멈추면, Spring 컨텍스트 종료(create-drop 의 DROP)가 그 뒤에
 * 일어나 죽은 컨테이너 연결을 기다리다 타임아웃하므로 그 패턴을 피한다.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class PostgresContainerTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true"); // 검색 섬(V1) 생성
    }
}
