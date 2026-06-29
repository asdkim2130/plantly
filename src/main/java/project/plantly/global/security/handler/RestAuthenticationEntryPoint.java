package project.plantly.global.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import project.plantly.global.exception.CommonErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;


// 인증되지 않은 사용자가 보호된 자원에 접근했을 때(필터 단계) 401을 동일 포맷으로 응답.

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        SecurityResponseWriter.write(response, objectMapper, CommonErrorCode.UNAUTHORIZED);
    }
}