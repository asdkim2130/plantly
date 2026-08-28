package project.plantly.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 업로드 용량 초과. 도메인 코드가 아니라 여기 있는 이유는 이 한계가 두 층에서 걸리기 때문이다 —
    // 서블릿 상한(spring.servlet.multipart.max-file-size)은 컨트롤러에 닿기도 전에 터지고(전역),
    // 애플리케이션 상한(app.upload.max-file-size)은 UploadService 가 확인한다. 같은 사실을 두 문장으로
    // 말하지 않으려고 한 곳에 둔다. 413 이 아니라 400 인 것은 "입력 오류는 400" 이라는 이 API 의 규칙에 맞춘 것.
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "업로드 가능한 파일 용량을 초과했습니다."),

    // 요청 Content-Type 이 엔드포인트가 받는 형식과 다르거나 아예 없는 경우.
    // 사용자가 화면에서 만들 수 있는 상태가 아니라 클라이언트 구현 오류라, 메시지는 일반화하고
    // 실제 형식/허용 형식은 로그로 남긴다(GlobalExceptionHandler).
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
