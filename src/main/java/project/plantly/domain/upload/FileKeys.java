package project.plantly.domain.upload;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 저장 파일의 식별자(key) 생성과 검증.
 *
 * <p>key 는 {@code {UUID 32자리 소문자 hex}.{확장자}} 한 덩어리이고 <b>구분자('/')를 포함하지 않는다.</b>
 * 그래서 {@code GET /api/v1/files/{key}} 가 단일 경로 변수로 잡히고, 아래 정규식을 통과한 문자열에는
 * {@code ..} 도 {@code /} 도 들어 있을 수 없어 <b>경로 조작이 구조적으로 불가능하다.</b>
 * (걸러내는 방식이 아니라 형태를 못 박는 방식이다 — 인코딩 우회를 하나씩 쫓지 않아도 된다.)
 *
 * <p>클라이언트가 보낸 원본 파일명은 key 에 쓰지 않는다. 경로 조작·한글/공백·중복·길이 제한을
 * 한꺼번에 없애는 가장 싼 방법이고, 원본 이름이 필요해지면 그때 응답 메타로 따로 돌려주면 된다.
 *
 * <p>디렉터리 하나에 파일이 수십만 개 쌓이는 문제는 저장 구현이 key 앞 두 글자로 내부 샤딩해 푼다.
 * <b>샤딩은 URL 에 드러나지 않으므로</b> 저장 방식을 바꿔도 이미 발급된 URL 이 깨지지 않는다.
 */
public final class FileKeys {

    // 확장자를 enum 에서 뽑지 않고 정규식에 박은 이유: 정규식은 '형태'만 거르고, 그 확장자가 실제로
    // 지원 형식인지는 parse() 가 ImageFormat 에 물어본다. 두 검사가 겹치지만 순서가 다르다.
    private static final Pattern KEY_PATTERN = Pattern.compile("^[0-9a-f]{32}[.][a-z0-9]{2,5}$");

    private FileKeys() {
    }

    /** 새 파일의 key 를 만든다. 같은 내용을 두 번 올려도 다른 key 가 나온다(중복 제거는 하지 않는다). */
    public static String generate(ImageFormat format) {
        String id = UUID.randomUUID().toString().replace("-", "");
        return id + "." + format.getExtension();
    }

    /**
     * 요청으로 들어온 key 를 검증하고 그 형식을 돌려준다.
     *
     * @return 형태와 확장자가 모두 유효하면 해당 형식, 아니면 비어 있음(= 그런 파일은 존재할 수 없다)
     */
    public static Optional<ImageFormat> parse(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            return Optional.empty();
        }
        return ImageFormat.fromExtension(key.substring(key.lastIndexOf('.') + 1));
    }

    /** 저장 구현이 쓰는 샤드 이름(key 앞 두 글자). {@link #parse} 를 통과한 key 에만 쓴다. */
    public static String shardOf(String key) {
        return key.substring(0, 2);
    }
}
