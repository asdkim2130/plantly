package project.plantly.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import project.plantly.global.exception.ErrorCode;

import java.util.List;

/**
 * 모든 응답의 봉투.
 *
 * <p>{@code code} 는 클라이언트가 <b>분기에 쓰는</b> 식별자다({@code error} 는 사람이 읽는 문구다).
 * 문구로 분기하면 문구를 다듬는 순간 클라이언트가 깨지고, 상태 코드만으로는 갈라지지 않는다 —
 * 회사 등록 경로의 400 은 의미가 15가지쯤 되고 그중 {@code VERIFICATION_EXPIRED} 만 "인증 단계로
 * 되돌려야 하는" 400 이다. 값은 {@link ErrorCode} 구현 enum 의 상수 이름 그대로다.
 *
 * <p>그래서 실패 응답을 만드는 통로는 {@link ErrorCode} 를 받는 것 하나뿐이다. 문구만 받는 팩토리를
 * 남겨두면 코드 없는 실패 응답이 생기고, 그 경로만 클라이언트가 분기할 수 없게 된다.
 *
 * <p>성공 응답에는 {@code code} 가 없다(null → 직렬화에서 빠짐). 성공은 갈래가 하나라 분기할 것이 없다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 필드는 JSON에서 빠짐
public record ApiResponse<T>(boolean success, String message, T data, String code, String error,
                             List<ValidationError> errors) {

    // 메시지 + 데이터 (예: 로그인)
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, null, null);
    }

    // 데이터만 (예: 단순 조회)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data, null, null, null);
    }

    // 메시지만 (예: 회원가입 완료)
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null, null, null, null);
    }

    // 성공 플래그만 (예: 수정 반영 — 화면에 이미 보이는 값이라 본문/메시지 불필요)
    // (record 컴포넌트 accessor success() 와 충돌하지 않도록 ok() 로 둔다)
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null, null, null);
    }

    /** 실패 응답. 코드와 문구가 같은 출처에서 나오므로 둘이 어긋날 수 없다. */
    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return failure(errorCode, errorCode.getMessage());
    }

    /**
     * 문구만 따로 정하는 실패 응답. 코드가 정한 기본 문구를 쓸 수 없는 두 경우에만 쓴다 —
     * 검증 실패(첫 위반 메시지를 싣는다)와, 예외가 {@code @ResponseStatus(reason=...)} 로 문구를
     * 직접 지정한 경우다.
     */
    public static ApiResponse<Void> failure(ErrorCode errorCode, String error) {
        return new ApiResponse<>(false, null, null, errorCode.name(), error, null);
    }

    /**
     * 입력 검증 실패. 위반을 전부 담아 내려보낸다.
     *
     * <p>하나씩 알려주지 않는 이유는 회사 등록 폼처럼 입력 항목이 30종인 화면 때문이다. 한 번에 하나만
     * 지적하면 사용자가 저장을 몇 번이나 눌러야 하고, 그때마다 화면이 다시 그려진다. 프론트는 각 항목
     * 아래에 메시지를 붙이고 첫 항목으로 스크롤·포커스를 옮긴다.
     *
     * <p>{@code errors} 는 화면의 폼 순서(DTO 선언 순서)로 정렬돼 있으므로 첫 원소가 곧 스크롤 대상이다 —
     * 프론트가 필드명을 자기 DOM 순서에 다시 매핑할 필요가 없다(ValidationErrorOrdering).
     *
     * <p>{@code error} 에는 그 첫 메시지를 그대로 넣는다. 검증 실패가 아닌 응답과 봉투 모양을 맞춰,
     * 항목별 표시를 하지 않는 화면(관리자 도구 등)이 {@code error} 하나만 읽어도 동작하게 하기 위해서다.
     * {@code code} 는 항목이 아니라 폼 전체의 판정이라 위반마다 달라지지 않는다(항상 INVALID_INPUT) —
     * 어느 칸이 왜 틀렸는지는 {@code errors[].field} 가 말한다.
     */
    public static ApiResponse<Void> failure(ErrorCode errorCode, String error, List<ValidationError> errors) {
        return new ApiResponse<>(false, null, null, errorCode.name(), error, errors);
    }
}
