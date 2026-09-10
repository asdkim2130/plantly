package project.plantly.global.meta.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 폼 하나의 입력 제약 전체.
 *
 * <p>{@code fields} 는 DTO 선언 순서로 정렬돼 있고, 그 순서가 곧 화면의 폼 순서다 —
 * 검증 실패 응답의 {@code errors} 정렬 기준과 같은 근거({@code record.getRecordComponents()})를 쓴다.
 * 덕분에 프론트는 이 목록을 위에서 아래로 훑는 것만으로 폼과 같은 순서를 얻는다.
 *
 * <p>{@code rules} 는 필드에 붙지 않는 폼 전체 규칙이다(여러 필드를 함께 보는 클래스 레벨 제약).
 * 지금 열려 있는 폼에는 없어 대개 응답에서 키가 빠진다.
 *
 * <p>제약이 하나도 없는 필드는 목록에 나오지 않는다. 검증이 걸리지 않는 자리이기 때문이며,
 * enum 필드({@code trlLevel} 등)가 여기 해당한다 — 선택지 목록은 제약이 아니라 옵션 조회
 * ({@code GET /api/v1/meta/options}, {@link project.plantly.global.meta.OptionCatalog})의 몫이다.
 * 두 응답은 같은 필드 이름을 키로 쓰므로 프론트가 칸 이름 하나로 이어 붙일 수 있다.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record FormConstraintsResponse(
        String form,
        List<ConstraintRule> rules,
        List<FieldConstraints> fields
) {
}
