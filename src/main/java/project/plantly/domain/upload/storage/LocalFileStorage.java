package project.plantly.domain.upload.storage;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import project.plantly.domain.upload.FileKeys;
import project.plantly.domain.upload.ImageFormat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * 로컬 디스크 저장 구현.
 *
 * <p>파일은 {@code {baseDir}/{key 앞 두 글자}/{key}} 에 놓인다. 샤딩을 두는 이유는 디렉터리 하나에
 * 파일이 수십만 개 쌓이면 파일시스템에 따라 조회가 급격히 느려지기 때문이고, key 가 hex 라 256개
 * 디렉터리에 고르게 흩어진다. <b>이 배치는 URL 에 드러나지 않으므로</b> 나중에 바꿔도 발급된 URL 이 산다.
 *
 * <p>IO 실패는 {@link UncheckedIOException} 으로 던지고 사용자 응답으로 번역하지 않는다 —
 * 어떤 상태 코드로 나갈지는 저장소가 아니라 서비스가 정할 일이다.
 */
public class LocalFileStorage implements FileStorage {

    private final Path baseDir;

    public LocalFileStorage(Path baseDir) {
        this.baseDir = baseDir.toAbsolutePath().normalize();
        try {
            // 첫 업로드 때가 아니라 부팅 때 만든다. 권한/마운트 문제를 사용자 요청이 아니라 기동에서 드러낸다.
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리를 만들 수 없습니다: " + this.baseDir, e);
        }
    }

    @Override
    public String store(byte[] content, ImageFormat format) {
        String key = FileKeys.generate(format);
        Path target = resolve(key)
                .orElseThrow(() -> new IllegalStateException("생성한 key 가 검증을 통과하지 못했습니다: " + key));
        try {
            Files.createDirectories(target.getParent());
            // CREATE_NEW: 이미 있으면 실패한다. UUID 충돌은 사실상 없지만, 있다면 조용히 덮어써
            // 남의 파일을 날리는 것보다 실패하는 편이 낫다.
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException("파일을 저장하지 못했습니다: " + key, e);
        }
        return key;
    }

    @Override
    public Optional<Resource> load(String key) {
        return resolve(key)
                .filter(Files::isRegularFile)
                .map(FileSystemResource::new);
    }

    @Override
    public void delete(String key) {
        Optional<Path> target = resolve(key);
        if (target.isEmpty()) {
            return;
        }
        try {
            Files.deleteIfExists(target.get());
        } catch (IOException e) {
            throw new UncheckedIOException("파일을 삭제하지 못했습니다: " + key, e);
        }
    }

    /**
     * key 를 디스크 경로로 옮긴다. 형태가 유효하지 않으면 빈 값이다.
     *
     * <p>{@code FileKeys} 의 정규식이 이미 구분자와 {@code ..} 를 배제하지만, 여기서 한 번 더
     * baseDir 안쪽인지 확인한다. 저장 구현은 신뢰 경계의 마지막 층이라 상위 검증을 전제하지 않는다.
     */
    private Optional<Path> resolve(String key) {
        if (FileKeys.parse(key).isEmpty()) {
            return Optional.empty();
        }
        Path resolved = baseDir.resolve(FileKeys.shardOf(key)).resolve(key).normalize();
        if (!resolved.startsWith(baseDir)) {
            return Optional.empty();
        }
        return Optional.of(resolved);
    }
}
