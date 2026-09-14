package project.plantly.domain.upload.storage;

import jakarta.servlet.MultipartConfigElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
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

    /**
     * 서블릿 multipart 상한. {@code app.upload.max-file-size} 에서 파일·요청 상한을 함께 만든다.
     *
     * <p>스프링 부트의 기본 빈({@code MultipartAutoConfiguration})은 이 빈이 없을 때만 생기므로 이것이 그 자리를
     * 대신한다. 그래서 <b>{@code spring.servlet.multipart.max-*}·{@code location}·{@code file-size-threshold}
     * 는 읽히지 않는다.</b> 상한 두 개를 거기서 따로 받으면 업로드 제약 API 가 알려주는 값과 서블릿이 실제로
     * 끊는 값이 갈릴 수 있다 — 그 조합을 설정으로 만들 수 없게 하는 것이 이 빈의 목적이다. 나머지 둘은
     * 쓰지 않던 항목이고 기본값(임시 디렉터리·임계값 0)이 그대로 유지된다.
     */
    @Bean
    public MultipartConfigElement multipartConfigElement(StorageProperties properties) {
        log.info("업로드 상한: 파일 {} / 요청 {}", properties.maxFileSize(), properties.maxRequestSize());

        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(properties.maxFileSize());
        factory.setMaxRequestSize(properties.maxRequestSize());
        return factory.createMultipartConfig();
    }
}
