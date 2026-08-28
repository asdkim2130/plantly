package project.plantly.domain.upload.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

/**
 * 업로드 저장 설정.
 *
 * <p>허용 이미지 형식은 여기 없다 — {@code ImageFormat} enum 이 정본이다(그 클래스의 주석 참고).
 *
 * <p>{@code maxFileSize} 는 {@code spring.servlet.multipart.max-file-size} 와 <b>같은 값으로 맞춰 둔다.</b>
 * 둘을 다 두는 이유는 층이 다르기 때문이다 — 서블릿 상한은 요청을 다 받기 전에 끊어 거대한 본문을
 * 버퍼링하지 않게 하고(방어), 이 값은 서비스가 스스로 확인하는 최종 경계다(계약). 서블릿 상한만
 * 두면 서비스를 직접 호출하는 경로에는 아무 제한이 없고, 이 값만 두면 10GB 짜리 요청도 일단 다 받는다.
 */
@ConfigurationProperties(prefix = "app.upload")
public record StorageProperties(
        DataSize maxFileSize,
        Local local
) {

    private static final DataSize DEFAULT_MAX_FILE_SIZE = DataSize.ofMegabytes(10);

    public StorageProperties {
        if (maxFileSize == null) maxFileSize = DEFAULT_MAX_FILE_SIZE;
        if (local == null) local = new Local(null);
    }

    /** 로컬 디스크 저장 구현 설정. 저장소가 바뀌면 형제 블록이 늘어난다(app.upload.s3.* 등). */
    public record Local(Path baseDir) {

        private static final Path DEFAULT_BASE_DIR = Path.of("./uploads");

        public Local {
            if (baseDir == null) baseDir = DEFAULT_BASE_DIR;
        }
    }
}
