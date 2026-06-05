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

    public static ApiResponse<Void> failure(String error) {
        return new ApiResponse<>(false, null, null, error);
    }
}
