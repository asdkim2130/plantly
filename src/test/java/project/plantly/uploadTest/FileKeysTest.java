package project.plantly.uploadTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import project.plantly.domain.upload.FileKeys;
import project.plantly.domain.upload.ImageFormat;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("파일 key - 형태를 못 박아 경로 조작을 구조적으로 막는다")
class FileKeysTest {

    @Test
    @DisplayName("생성한 key 는 UUID 32자리 hex + 형식 확장자이고, 다시 파싱하면 같은 형식이 나온다")
    void generate_roundTrip() {
        for (ImageFormat format : ImageFormat.values()) {
            String key = FileKeys.generate(format);

            assertThat(key).matches("[0-9a-f]{32}\\." + format.getExtension());
            assertThat(FileKeys.parse(key)).contains(format);
        }
    }

    @Test
    @DisplayName("같은 형식을 두 번 생성해도 key 가 겹치지 않는다 - 같은 그림을 다시 올리면 새 URL 이다")
    void generate_isUnique() {
        assertThat(FileKeys.generate(ImageFormat.PNG)).isNotEqualTo(FileKeys.generate(ImageFormat.PNG));
    }

    @ParameterizedTest
    @DisplayName("경로 조작을 노린 key 는 파싱되지 않는다 - 걸러내는 게 아니라 형태가 아예 다르다")
    @ValueSource(strings = {
            "../../../etc/passwd",
            "..%2F..%2Fetc%2Fpasswd",
            "ab/cd.png",
            "/etc/passwd",
            "....//aaaaaaaaaaaaaaaaaaaaaaaaaaaa.png",
    })
    void parse_rejectsTraversal(String key) {
        assertThat(FileKeys.parse(key)).isEmpty();
    }

    @ParameterizedTest
    @DisplayName("형태나 확장자가 규약과 다르면 파싱되지 않는다")
    @ValueSource(strings = {
            "0123456789abcdef0123456789abcde.png",     // 31자리 - 짧다
            "0123456789abcdef0123456789abcdef0.png",   // 33자리 - 길다
            "0123456789ABCDEF0123456789ABCDEF.png",    // 대문자 hex 는 발급하지 않는다
            "0123456789abcdef0123456789abcdef.gif",    // 허용 형식이 아니다
            "0123456789abcdef0123456789abcdef.svg",    // 스크립트를 품을 수 있어 제외한 형식
            "0123456789abcdef0123456789abcdef",        // 확장자 없음
            "0123456789abcdef0123456789abcdef.png.exe",
            "",
    })
    void parse_rejectsMalformed(String key) {
        assertThat(FileKeys.parse(key)).isEmpty();
    }

    @Test
    @DisplayName("null key 도 예외 없이 빈 값이다 - 없는 파일과 같은 결과로 모은다")
    void parse_rejectsNull() {
        assertThat(FileKeys.parse(null)).isEmpty();
    }

    @Test
    @DisplayName("샤드는 key 앞 두 글자다 - 이 배치는 URL 에 드러나지 않는다")
    void shardOf() {
        assertThat(FileKeys.shardOf("9f3a1c7e5b2d4a018e6f0c9b7d3a5e21.webp")).isEqualTo("9f");
    }
}
