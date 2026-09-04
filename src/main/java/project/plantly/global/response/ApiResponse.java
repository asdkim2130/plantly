package project.plantly.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)  // null 필드는 JSON에서 빠짐
public record ApiResponse<T>(boolean success, String message, T data, String error,
                             List<ValidationError> errors) {

    // 메시지 + 데이터 (예: 로그인)
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, null);
    }

    // 데이터만 (예: 단순 조회)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data, null, null);
    }

    // 메시지만 (예: 회원가입 완료)
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null, null, null);
    }

    // 성공 플래그만 (예: 수정 반영 — 화면에 이미 보이는 값이라 본문/메시지 불필요)
    // (record 컴포넌트 accessor success() 와 충돌하지 않도록 ok() 로 둔다)
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null, null);
    }

    public static ApiResponse<Void> failure(String error) {
        return new ApiResponse<>(false, null, null, error, null);
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
     */
    public static ApiResponse<Void> failure(String error, List<ValidationError> errors) {
        return new ApiResponse<>(false, null, null, error, errors);
    }
}
