package project.plantly.domain.upload.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

/**
 * 업로드 저장 설정.
 *
 * <p>허용 이미지 형식은 여기 없다 — {@code ImageFormat} enum 이 정본이다(그 클래스의 주석 참고).
 *
 * <p><b>용량 상한은 {@code maxFileSize} 하나만 설정한다.</b> 서블릿 multipart 상한(파일·요청)도 이 값에서
 * 도출한다({@link StorageConfig#multipartConfigElement}). 층은 둘이다 — 서블릿 상한은 요청을 다 받기 전에
 * 끊어 거대한 본문을 버퍼링하지 않게 하고(방어), 이 값은 서비스가 스스로 확인하는 최종 경계다(계약).
 * 층은 둘이어도 <b>출처는 하나여야 한다.</b> 이 값은 업로드 제약 API 로 프론트에 그대로 나가는데,
 * 서블릿 상한을 따로 설정하게 두면 파일 상한만 올린 배포에서 프론트가 통과시킨 파일을 서블릿이 끊는다.
 */
@ConfigurationProperties(prefix = "app.upload")
public record StorageProperties(
        DataSize maxFileSize,
        Local local
) {

    private static final DataSize DEFAULT_MAX_FILE_SIZE = DataSize.ofMegabytes(10);

    /**
     * multipart 본문에서 파일 바이트 말고 붙는 것(boundary 줄, 파트 헤더의 파일명·Content-Type)의 여유분.
     * 실제로는 수백 바이트~수 KB 인데 넉넉히 잡는다 — 모자라면 상한 근처 파일이 파일 검사는 통과하고
     * 요청 검사에서 끊기고, 남는 것은 비용이 없다(파일 파트 자체는 서블릿 파일 상한이 따로 막는다).
     */
    static final DataSize MULTIPART_OVERHEAD = DataSize.ofMegabytes(1);

    public StorageProperties {
        if (maxFileSize == null) maxFileSize = DEFAULT_MAX_FILE_SIZE;
        // 서블릿 multipart 설정에서 음수는 '무제한'이다. 이 값을 거기로 흘려보내므로 잘못 넣은 값이
        // 조용히 상한 해제가 되지 않게 기동 시점에 끊는다(0 이면 모든 업로드가 거절되는 설정이라 같이 막는다).
        if (maxFileSize.toBytes() <= 0) {
            throw new IllegalArgumentException(
                    "app.upload.max-file-size 는 0보다 커야 합니다: " + maxFileSize);
        }
        if (local == null) local = new Local(null);
    }

    /**
     * multipart 요청 본문 상한. 파일 상한 + {@link #MULTIPART_OVERHEAD}.
     * 설정 항목으로 두지 않는 이유는 클래스 주석 참고 — 이 관계가 깨지는 조합을 설정할 수 없게 한다.
     */
    public DataSize maxRequestSize() {
        return DataSize.ofBytes(maxFileSize.toBytes() + MULTIPART_OVERHEAD.toBytes());
    }

    /** 로컬 디스크 저장 구현 설정. 저장소가 바뀌면 형제 블록이 늘어난다(app.upload.s3.* 등). */
    public record Local(Path baseDir) {

        private static final Path DEFAULT_BASE_DIR = Path.of("./uploads");

        public Local {
            if (baseDir == null) baseDir = DEFAULT_BASE_DIR;
        }
    }
}
