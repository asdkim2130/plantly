package project.plantly.global.seed;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 시드 설정 배선. 이 패키지의 컴포넌트는 전부 {@code seed} 프로파일에서만 뜬다.
 */
@Configuration
@Profile(SeedConfig.PROFILE)
@EnableConfigurationProperties(SeedProperties.class)
public class SeedConfig {

    /** 시드 프로파일 이름. 각 시드 컴포넌트가 같은 상수를 참조해 오타로 반쪽만 뜨는 일을 막는다. */
    public static final String PROFILE = "seed";
}
