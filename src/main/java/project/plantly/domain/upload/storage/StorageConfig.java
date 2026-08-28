package project.plantly.domain.upload.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 저장소 구현 배선. 지금은 후보가 하나뿐이라 분기가 없지만, S3/Supabase 구현이 생기면
 * {@code NtsConfig} 처럼 설정값으로 하나만 골라 올리는 자리가 여기다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public FileStorage fileStorage(StorageProperties properties) {
        log.info("파일 저장소: 로컬 디스크 ({})", properties.local().baseDir().toAbsolutePath().normalize());
        return new LocalFileStorage(properties.local().baseDir());
    }
}
