package project.plantly.uploadTest;

import java.io.ByteArrayOutputStream;

/**
 * 테스트용 이미지 바이트 생성기.
 *
 * <p>실제 이미지 파일을 리소스로 두지 않는 이유는, 검증하려는 것이 "디코딩되는 그림인가"가 아니라
 * <b>매직 바이트 판별이 맞는가</b>이기 때문이다. 헤더만 진짜인 바이트가 오히려 이 테스트의 주제에 가깝다
 * (위조 Content-Type 케이스도 같은 도구로 만든다).
 */
final class ImageBytes {

    private ImageBytes() {
    }

    static byte[] jpeg() {
        return withPadding(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
    }

    static byte[] png() {
        return withPadding(pngSignature());
    }

    static byte[] pngSignature() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};
    }

    /** RIFF 컨테이너: 0~3 "RIFF", 4~7 길이(아무 값), 8~11 "WEBP". */
    static byte[] webp() {
        return riff(new byte[]{'W', 'E', 'B', 'P'});
    }

    /** RIFF 이지만 WEBP 가 아닌 컨테이너(예: WAV). 앞 4바이트만 보고 통과시키면 안 된다. */
    static byte[] riffButNotWebp() {
        return riff(new byte[]{'W', 'A', 'V', 'E'});
    }

    /** 이미지가 아닌 바이트. 확장자나 Content-Type 을 이미지로 붙여도 통과하면 안 된다. */
    static byte[] notAnImage() {
        return "MZ this is not an image at all".getBytes();
    }

    private static byte[] riff(byte[] formType) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{'R', 'I', 'F', 'F'});
        out.writeBytes(new byte[]{(byte) 0x10, 0, 0, 0});
        out.writeBytes(formType);
        out.writeBytes(new byte[]{0, 0, 0, 0});
        return out.toByteArray();
    }

    private static byte[] withPadding(byte[] signature) {
        byte[] result = new byte[Math.max(signature.length, 32)];
        System.arraycopy(signature, 0, result, 0, signature.length);
        return result;
    }
}
