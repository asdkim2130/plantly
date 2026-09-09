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
 * 문자열이냐 컬렉션이냐에 따라 {@code maxLength} / {@code maxItems} 로 갈린다 — 한쪽은 입력칸의
 * {@code maxlength} 속성이고 다른 쪽은 '더 추가' 버튼의 비활성 조건이라 화면에서 쓰임이 전혀 다르다.
 *
 * <p>{@code value} 는 항상 숫자이거나 없다. 여기 실리는 규칙이 전부 "우리가 정한 수" 이기 때문이며
 * ({@link project.plantly.global.meta.FormConstraintsReader} 참고), 값이 필요 없는 규칙
 * ({@code required})에서는 키가 빠진다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConstraintRule(String type, Number value, String message) {

    public static ConstraintRule of(String type, Number value, String message) {
        return new ConstraintRule(type, value, message);
    }

    public static ConstraintRule of(String type, String message) {
        return new ConstraintRule(type, null, message);
    }
}
