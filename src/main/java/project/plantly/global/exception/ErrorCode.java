package project.plantly.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 하나의 정의: 상태 코드 · 사용자 문구 · <b>기계가 읽는 식별자</b>.
 *
 * <p>{@code name()} 을 계약에 넣는 이유는 응답의 {@code code} 가 여기서 나오기 때문이다. 구현체가 전부
 * enum 이라 상수 이름이 이미 안정적인 식별자이고({@code VERIFICATION_EXPIRED} 등), 별도 문자열 필드를
 * 두면 상수 이름과 코드가 갈릴 자리가 생긴다. enum 은 이 메서드를 자동으로 만족하므로 구현할 것이 없다.
 *
 * <p>그래서 <b>상수 이름을 바꾸는 것은 API 계약을 바꾸는 것</b>이다 — 클라이언트가 그 문자열로 분기한다.
 */
public interface ErrorCode {

    HttpStatus getStatus();

    String getMessage();

    /** 응답 {@code code} 로 나가는 식별자. enum 상수 이름 그대로다. */
    String name();
}
