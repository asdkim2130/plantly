package project.plantly.domain.upload;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/**
 * 업로드를 허용하는 이미지 형식과 그 판별.
 *
 * <p><b>허용 목록을 설정값이 아니라 이 enum 에 두는 이유</b> — 형식을 하나 늘리려면 아래 시그니처 판별
 * 코드를 반드시 함께 짜야 한다. 설정으로 켤 수 있는 형식은 결국 여기 구현된 것의 부분집합뿐이라,
 * 설정 항목을 두면 "켰는데 안 되는" 조합만 생기고 볼 곳이 두 군데로 갈린다.
 *
 * <p>GIF 는 제외했다(제조업 디렉터리에 애니메이션이 쓰일 자리가 없고 용량만 크다). SVG 도 제외했다 —
 * SVG 는 스크립트를 품을 수 있어 이미지가 아니라 실행 가능한 문서에 가깝다. 브라우저가 같은 오리진에서
 * 그걸 렌더하면 저장형 XSS 가 된다. 벡터 로고가 필요해지면 별도 경로로 다룬다.
 */
@Getter
@RequiredArgsConstructor
public enum ImageFormat {

    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    private final String contentType;
    private final String extension;

    // 판별에 필요한 최소 길이(WebP 의 12바이트)보다 짧으면 볼 것도 없이 실패다.
    private static final int MIN_HEADER_LENGTH = 12;

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE =
            {(byte) 0x89, 'P', 'N', 'G', (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};
    private static final byte[] RIFF_SIGNATURE = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_SIGNATURE = {'W', 'E', 'B', 'P'};

    /**
     * 파일 내용의 앞부분(매직 바이트)만 보고 실제 형식을 판별한다.
     *
     * <p><b>클라이언트가 보낸 Content-Type 과 파일명 확장자를 보지 않는 것이 요점이다.</b> 둘 다 요청에
     * 실려 오는 값이라 위조할 수 있다 — 확장자만 믿으면 {@code .png} 로 이름만 바꾼 임의의 바이트가
     * 우리 도메인에서 서빙된다.
     *
     * <p>{@code ImageIO.read()} 로 실제 디코딩해 보는 방법도 있지만 Java 표준에 WebP 리더가 없어
     * 허용 형식 중 하나가 통째로 튕긴다. 시그니처 검사는 의존성 없이 세 형식을 모두 정확히 가른다.
     *
     * @return 허용 형식이면 그 값, 아니면 비어 있음
     */
    public static Optional<ImageFormat> detect(byte[] content) {
        if (content == null || content.length < MIN_HEADER_LENGTH) {
            return Optional.empty();
        }
        if (startsWith(content, 0, JPEG_SIGNATURE)) {
            return Optional.of(JPEG);
        }
        if (startsWith(content, 0, PNG_SIGNATURE)) {
            return Optional.of(PNG);
        }
        // WebP 는 RIFF 컨테이너라 두 곳을 봐야 한다. 4~7바이트는 파일 길이 필드라 값이 매번 달라 건너뛴다.
        if (startsWith(content, 0, RIFF_SIGNATURE) && startsWith(content, 8, WEBP_SIGNATURE)) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    /** 저장된 key 의 확장자로 서빙 시 Content-Type 을 되찾는다. */
    public static Optional<ImageFormat> fromExtension(String extension) {
        return Arrays.stream(values())
                .filter(format -> format.extension.equals(extension))
                .findFirst();
    }

    private static boolean startsWith(byte[] content, int offset, byte[] signature) {
        for (int i = 0; i < signature.length; i++) {
            if (content[offset + i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
