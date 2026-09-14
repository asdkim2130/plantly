package project.plantly.uploadTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import project.plantly.domain.upload.ImageFormat;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("이미지 형식 판별 - 클라이언트가 말한 형식이 아니라 실제 바이트를 본다")
class ImageFormatTest {

    @Test
    @DisplayName("JPEG·PNG·WebP 의 매직 바이트를 각각 판별한다")
    void detect_supportedFormats() {
        assertThat(ImageFormat.detect(ImageBytes.jpeg())).contains(ImageFormat.JPEG);
        assertThat(ImageFormat.detect(ImageBytes.png())).contains(ImageFormat.PNG);
        assertThat(ImageFormat.detect(ImageBytes.webp())).contains(ImageFormat.WEBP);
    }

    @Test
    @DisplayName("이미지가 아닌 바이트는 판별되지 않는다 - 확장자를 .png 로 바꿔 올려도 통과하면 안 된다")
    void detect_rejectsNonImage() {
        assertThat(ImageFormat.detect(ImageBytes.notAnImage())).isEmpty();
    }

    @Test
    @DisplayName("RIFF 이지만 WEBP 가 아닌 컨테이너(WAV 등)는 거른다 - 앞 4바이트만 보면 통과해 버린다")
    void detect_rejectsRiffThatIsNotWebp() {
        assertThat(ImageFormat.detect(ImageBytes.riffButNotWebp())).isEmpty();
    }

    @Test
    @DisplayName("판별에 필요한 길이(12바이트)보다 짧거나 null 이면 판별되지 않는다")
    void detect_rejectsTooShortOrNull() {
        assertThat(ImageFormat.detect(null)).isEmpty();
        assertThat(ImageFormat.detect(new byte[0])).isEmpty();
        // PNG 시그니처 8바이트만 있는 경우 - 시그니처는 맞지만 최소 길이에 못 미친다.
        assertThat(ImageFormat.detect(ImageBytes.pngSignature())).isEmpty();
    }

    @Test
    @DisplayName("확장자로 형식을 되찾는다 - 저장된 key 에서 서빙 Content-Type 을 정할 때 쓴다")
    void fromExtension() {
        assertThat(ImageFormat.fromExtension("jpg")).contains(ImageFormat.JPEG);
        assertThat(ImageFormat.fromExtension("png")).contains(ImageFormat.PNG);
        assertThat(ImageFormat.fromExtension("webp")).contains(ImageFormat.WEBP);

        // 허용 목록 밖. gif/svg 를 뺀 결정이 여기서도 지켜져야 한다.
        assertThat(ImageFormat.fromExtension("gif")).isEmpty();
        assertThat(ImageFormat.fromExtension("svg")).isEmpty();
        // 우리가 발급하는 확장자는 jpg 하나뿐이라 jpeg 는 존재할 수 없는 key 다.
        // 입력으로는 jpeg 를 받지만(acceptedExtensions) 그 별칭이 key 해석으로 새면 안 된다.
        assertThat(ImageFormat.fromExtension("jpeg")).isEmpty();
        assertThat(ImageFormat.fromExtension("jfif")).isEmpty();
        assertThat(ImageFormat.fromExtension("PNG")).isEmpty();
    }

    @Test
    @DisplayName("입력 허용 확장자는 저장 확장자를 포함하고, JPEG 는 사용자 파일에 흔한 별칭까지 받는다")
    void acceptedExtensions() {
        // 저장 확장자가 빠지면 우리가 발급한 key 와 같은 이름의 파일을 프론트가 막는다.
        for (ImageFormat format : ImageFormat.values()) {
            assertThat(format.getAcceptedExtensions()).contains(format.getExtension());
            // 계약상 소문자·점 없음. 프론트는 이 형태를 전제로 대소문자를 무시해 비교한다.
            assertThat(format.getAcceptedExtensions()).allMatch(ext -> ext.matches("[a-z0-9]+"));
        }
        assertThat(ImageFormat.JPEG.getAcceptedExtensions()).containsExactly("jpg", "jpeg", "jpe", "jfif");
    }

    @Test
    @DisplayName("형식마다 Content-Type 과 확장자가 짝지어져 있다")
    void metadata() {
        assertThat(ImageFormat.JPEG.getContentType()).isEqualTo("image/jpeg");
        assertThat(ImageFormat.JPEG.getExtension()).isEqualTo("jpg");
        assertThat(ImageFormat.PNG.getContentType()).isEqualTo("image/png");
        assertThat(ImageFormat.WEBP.getContentType()).isEqualTo("image/webp");
        assertThat(ImageFormat.WEBP.getExtension()).isEqualTo("webp");
    }
}
