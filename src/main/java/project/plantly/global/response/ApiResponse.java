package project.plantly.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)  // null 필드는 JSON에서 빠짐
public record ApiResponse<T>(boolean success, String message, T data, String error) {

    // 메시지 + 데이터 (예: 로그인)
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    // 데이터만 (예: 단순 조회)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data, null);
    }

    // 메시지만 (예: 회원가입 완료)
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    // 성공 플래그만 (예: 수정 반영 — 화면에 이미 보이는 값이라 본문/메시지 불필요)
    // (record 컴포넌트 accessor success() 와 충돌하지 않도록 ok() 로 둔다)
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null);
    }

    public static ApiResponse<Void> failure(String error) {
        return new ApiResponse<>(false, null, null, error);
    }
}
