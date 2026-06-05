package project.plantly.global.security.handler;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import project.plantly.global.exception.ErrorCode;
import project.plantly.global.response.ApiResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


// 시큐리티 필터 단계(컨트롤러 진입 전)에서 발생한 인증/인가 실패를
// GlobalExceptionHandler와 동일한 ApiResponse 포맷의 JSON으로 직렬화한다.

final class SecurityResponseWriter {

    private SecurityResponseWriter() {
    }

    static void write(HttpServletResponse response, ObjectMapper objectMapper, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failure(errorCode.getMessage())));
    }
}