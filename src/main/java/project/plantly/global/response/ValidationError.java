package project.plantly.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 검증 실패 1건. 어느 입력칸이 왜 거절됐는지를 담는다.
 *
 * <p>{@code field} 는 요청 본문에서의 경로다 — {@code "companyName"} 같은 평면 이름이거나
 * {@code "contacts[0].phone"} 처럼 중첩 경로이며, 프론트는 이 값으로 해당 입력칸을 찾아 메시지를 붙인다.
 *
 * <p>필드에 붙지 않는 위반(여러 필드를 함께 보는 클래스 레벨 제약)은 {@code field} 가 없다.
 * 그때는 특정 칸이 아니라 폼 전체의 문제이므로 화면도 폼 단위로 표시해야 한다.
 * 직렬화에서 아예 빠지므로 클라이언트는 키 존재 여부로 두 경우를 구분한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationError(String field, String message) {

    public static ValidationError of(String field, String message) {
        return new ValidationError(field, message);
    }

    /** 필드에 붙지 않는 폼 전체 위반. */
    public static ValidationError form(String message) {
        return new ValidationError(null, message);
    }
}
