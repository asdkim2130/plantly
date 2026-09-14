package project.plantly.uploadTest;

import jakarta.servlet.MultipartConfigElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import project.plantly.domain.upload.storage.StorageConfig;
import project.plantly.domain.upload.storage.StorageProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 업로드 상한의 단일 출처.
 *
 * <p>업로드 제약 API 가 알려주는 값({@code app.upload.max-file-size})과 서블릿이 실제로 끊는 값이 갈리면
 * 프론트가 통과시킨 파일을 서버가 거절한다. 여기서는 서블릿 상한이 그 값에서 도출되는지를 보고,
 * 실제 서블릿 컨테이너에 걸리는지는 {@link UploadSizeLimitAcceptanceTest} 가 본다.
 */
@DisplayName("업로드 상한 설정 - 서블릿 상한은 파일 상한 하나에서 도출된다")
class StorageConfigTest {

    // 기본값(10MB)도, 옛 요청 상한 기본값(12MB)도 아닌 값.
    private static final DataSize FILE_LIMIT = DataSize.ofMegabytes(20);

    @Test
    @DisplayName("서블릿 파일 상한은 파일 상한과 같고, 요청 상한은 multipart 부가 용량만큼 더 크다")
    void multipartLimits_derivedFromFileLimit() {
        StorageProperties properties = new StorageProperties(FILE_LIMIT, null);

        MultipartConfigElement config = new StorageConfig().multipartConfigElement(properties);

        assertThat(config.getMaxFileSize()).isEqualTo(FILE_LIMIT.toBytes());
        // 요청 = 파일 + boundary·파트 헤더. 같거나 작으면 상한 근처 파일이 요청 검사에서 끊긴다.
        assertThat(config.getMaxRequestSize()).isEqualTo(properties.maxRequestSize().toBytes());
        assertThat(config.getMaxRequestSize()).isGreaterThan(config.getMaxFileSize());
    }

    @Test
    @DisplayName("요청 상한은 파일 상한 + 1MB 다")
    void maxRequestSize_isFileLimitPlusOverhead() {
        StorageProperties properties = new StorageProperties(FILE_LIMIT, null);

        assertThat(properties.maxRequestSize())
                .isEqualTo(DataSize.ofBytes(FILE_LIMIT.toBytes() + DataSize.ofMegabytes(1).toBytes()));
    }

    @Test
    @DisplayName("0 이하의 파일 상한은 기동 시점에 거절한다 - 서블릿 설정에서 음수는 '무제한'이다")
    void nonPositiveFileLimit_isRejected() {
        assertThatThrownBy(() -> new StorageProperties(DataSize.ofBytes(-1), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StorageProperties(DataSize.ofBytes(0), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
