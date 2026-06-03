package project.plantly.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)  // null 필드는 JSON에서 빠짐
public record ApiResponse(boolean success, String message, String error) {

    public static ApiResponse success(String message) {
        return new ApiResponse(true, message, null);
    }

    public static ApiResponse failure(String error) {
        return new ApiResponse(false, null, error);
    }
}
