package project.plantly.uploadTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import project.plantly.domain.upload.FileKeys;
import project.plantly.domain.upload.ImageFormat;
import project.plantly.domain.upload.storage.LocalFileStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("로컬 디스크 저장소")
class LocalFileStorageTest {

    @TempDir
    Path baseDir;

    @Test
    @DisplayName("저장한 파일을 key 로 다시 읽어 내용이 같다")
    void store_thenLoad() throws IOException {
        LocalFileStorage storage = new LocalFileStorage(baseDir);
        byte[] content = ImageBytes.png();

        String key = storage.store(content, ImageFormat.PNG);
        Optional<Resource> loaded = storage.load(key);

        assertThat(loaded).isPresent();
        assertThat(loaded.get().getContentAsByteArray()).isEqualTo(content);
    }

    @Test
    @DisplayName("파일은 key 앞 두 글자 디렉터리 아래에 놓인다 - 한 디렉터리에 파일이 몰리지 않게 한다")
    void store_shardsByKeyPrefix() {
        LocalFileStorage storage = new LocalFileStorage(baseDir);

        String key = storage.store(ImageBytes.jpeg(), ImageFormat.JPEG);

        Path expected = baseDir.resolve(FileKeys.shardOf(key)).resolve(key);
        assertThat(expected).exists();
    }

    @Test
    @DisplayName("없는 key 는 예외가 아니라 빈 값이다 - 404 로 바꾸는 것은 상위 층의 몫이다")
    void load_missingKey() {
        LocalFileStorage storage = new LocalFileStorage(baseDir);

        assertThat(storage.load("0123456789abcdef0123456789abcdef.png")).isEmpty();
    }

    @Test
    @DisplayName("경로 조작을 노린 key 는 baseDir 밖을 읽지 못한다 - 상위 검증을 전제하지 않는다")
    void load_rejectsTraversal() throws IOException {
        Path outside = baseDir.getParent().resolve("secret.txt");
        Files.writeString(outside, "top secret");
        LocalFileStorage storage = new LocalFileStorage(baseDir);

        assertThat(storage.load("../secret.txt")).isEmpty();
        assertThat(storage.load("..%2Fsecret.txt")).isEmpty();
        assertThat(outside).exists();
    }

    @Test
    @DisplayName("삭제하면 파일이 사라지고, 없는 key 를 지워도 조용히 지나간다")
    void delete() {
        LocalFileStorage storage = new LocalFileStorage(baseDir);
        String key = storage.store(ImageBytes.webp(), ImageFormat.WEBP);

        storage.delete(key);

        assertThat(storage.load(key)).isEmpty();
        storage.delete(key);
        storage.delete("0123456789abcdef0123456789abcdef.png");
    }

    @Test
    @DisplayName("baseDir 은 첫 업로드가 아니라 생성 시점에 만들어진다 - 권한 문제를 기동에서 드러낸다")
    void createsBaseDirOnConstruction() {
        Path notYetCreated = baseDir.resolve("nested").resolve("uploads");

        new LocalFileStorage(notYetCreated);

        assertThat(notYetCreated).isDirectory();
    }

    @Test
    @DisplayName("baseDir 자리에 파일이 있으면 기동에서 실패한다")
    void failsWhenBaseDirIsAFile() throws IOException {
        Path file = baseDir.resolve("not-a-directory");
        Files.writeString(file, "x");

        assertThatThrownBy(() -> new LocalFileStorage(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("업로드 디렉터리");
    }
}
