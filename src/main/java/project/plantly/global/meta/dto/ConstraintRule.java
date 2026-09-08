package project.plantly.global.meta.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 입력 규칙 1건. 서버 DTO 에 붙은 검증 애너테이션 하나가 규칙 하나로 옮겨진 것이다.
 *
 * <p>규칙 단위로 쪼개는 이유는 <b>메시지 때문</b>이다. 프론트가 저장 전에 막을 때와 서버가 400 으로
 * 되돌려줄 때 같은 문구가 떠야 하는데, 필드마다 {@code maxLength} 만 내려주면 문구는 프론트가 다시
 * 지어내야 한다. 규칙마다 자기 메시지를 달고 나가면 두 문구가 어긋날 자리가 없다.
 *
 * <p>{@code type} 은 애너테이션 이름이 아니라 화면이 쓰는 규칙 이름이다 —
 * {@code @NotNull}·{@code @NotBlank}·{@code @NotEmpty} 는 셋 다 {@code required} 로 나간다.
 * 프론트가 "값이 비었는가" 를 세 갈래로 나눌 이유가 없기 때문이다. 반대로 {@code @Size} 는 대상이
 * 문자열이냐 컬렉션이냐에 따라 {@code maxLength} / {@code maxItems} 로 갈린다.
 *
 * <p>모르는 애너테이션(커스텀 제약 등)은 이름을 소문자로 시작하게 바꿔 그대로 내보내고 값은 싣지 않는다.
 * 프론트는 모르는 {@code type} 을 무시하면 되고, 그래도 메시지는 잃지 않는다.
 *
 * <p>{@code value} 는 규칙에 따라 숫자({@code maxLength})이거나 문자열({@code pattern})이며,
 * 값이 필요 없는 규칙({@code required}, {@code email})에서는 키가 빠진다.
 *
 * <p>{@code pattern} 값은 자바 정규식 문자열이다. 지금 쓰는 패턴들은 자바/자바스크립트 문법이 같아
 * 프론트가 {@code new RegExp(pattern)} 으로 그대로 쓸 수 있지만, 앞으로 자바 전용 문법(가령 {@code \p{...}}
 * 확장)을 쓰는 패턴을 추가한다면 이 계약이 깨진다는 점은 알고 있어야 한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConstraintRule(String type, Object value, String message) {

    public static ConstraintRule of(String type, Object value, String message) {
        return new ConstraintRule(type, value, message);
    }

    public static ConstraintRule of(String type, String message) {
        return new ConstraintRule(type, null, message);
    }
}
