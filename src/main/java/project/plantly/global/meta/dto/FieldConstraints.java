package project.plantly.global.meta.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 입력칸 하나(또는 컬렉션 원소 하나)에 걸린 규칙 묶음.
 *
 * <p>{@code field} 는 요청 본문에서의 이름이라, 검증 실패 응답의
 * {@link project.plantly.global.response.ValidationError#field()} 와 같은 어휘다. 프론트는 두 응답을
 * 같은 키로 찾아 쓴다 — 제약은 폼을 그릴 때, 위반은 저장을 눌렀을 때.
 *
 * <p>중첩은 두 갈래다:
 * <ul>
 *   <li>{@code items} — 컬렉션 원소에 걸린 규칙. {@code tagNames} 의 "20자 이내" 는 리스트가 아니라
 *       원소의 제약이므로 여기 들어간다. 원소 서술자에는 이름이 없어 {@code field} 가 빠진다.</li>
 *   <li>{@code fields} — 중첩 객체의 하위 입력칸. {@code contacts} 처럼 객체 리스트면
 *       {@code items.fields} 에 담긴다({@code contacts[0].phone} 의 경로 모양 그대로).</li>
 * </ul>
 *
 * <p>규칙도 하위 필드도 없는 자리는 응답에서 키가 통째로 빠진다(빈 배열을 내려보내지 않는다).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record FieldConstraints(
        String field,
        List<ConstraintRule> rules,
        List<FieldConstraints> fields,
        FieldConstraints items
) {
}
