package project.plantly.global.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import project.plantly.global.exception.CommonErrorCode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;


// 인증은 됐으나 권한이 부족할 때(필터 단계) 403을 동일 포맷으로 응답.

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        SecurityResponseWriter.write(response, objectMapper, CommonErrorCode.FORBIDDEN);
    }
}