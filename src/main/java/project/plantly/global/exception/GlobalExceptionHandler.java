package project.plantly.global.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import project.plantly.global.response.ApiResponse;

import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 409, 500 중 의도된 비즈니스 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e){
        ErrorCode code = e.getErrorCode();

        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.failure(code.getMessage()));
    }

    // 400 검증실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e){
        String firstMessage = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

        return ResponseEntity.badRequest().body(ApiResponse.failure(firstMessage));
    }

    // 400 검증실패 - 메서드 파라미터 검증. 위 MethodArgumentNotValidException 은 "요청 본문 객체"를 바인딩·검증할 때
    // 나오고, 이쪽은 본문이 객체가 아니라 리스트라 파라미터 레벨 검증이 도는 경우(@Valid 의 원소 중첩 검증,
    // List<@NotNull X> 같은 컨테이너 원소 제약)에 나온다. 안 잡으면 아래 fallback 이 삼켜 400 이 500 으로 나간다.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException e) {
        String firstMessage = e.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(CommonErrorCode.INVALID_INPUT.getMessage());

        return ResponseEntity.badRequest().body(ApiResponse.failure(firstMessage));
    }

    // 400 업로드 용량 초과 - 서블릿 컨테이너가 요청을 다 받기 전에 끊고 던진다.
    // 컨트롤러에 닿지 않으므로 UploadService 의 상한 검사로는 잡히지 않고, 이 핸들러가 없으면
    // 아래 fallback 이 삼켜 "서버 오류"(500)로 나간다 - 사용자가 고칠 수 있는 문제인데 서버 탓으로 보인다.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(CommonErrorCode.FILE_TOO_LARGE.getStatus())
                .body(ApiResponse.failure(CommonErrorCode.FILE_TOO_LARGE.getMessage()));
    }

    // 415 요청 형식 불일치 - Content-Type 이 엔드포인트가 받는 형식과 다르거나 아예 없다.
    // 잡지 않으면 아래 fallback 이 삼켜 500 으로 나가고, 클라이언트 구현 오류가 "서버가 죽었다"로 읽힌다.
    // 스프링의 기본 처리(DefaultHandlerExceptionResolver)가 415 로 매핑해 주지만, 이 advice 의
    // @ExceptionHandler(Exception.class) 가 먼저 잡아 가로채기 때문에 여기서 명시적으로 되돌려 놓는다.
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        // 사용자가 고칠 수 있는 값이 아니므로 응답은 일반화하고, 진단에 필요한 값은 로그로만 남긴다.
        log.warn("지원하지 않는 Content-Type 요청: 받은 값={}, 허용={}", e.getContentType(), e.getSupportedMediaTypes());

        return ResponseEntity.status(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.getStatus())
                .body(ApiResponse.failure(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage()));
    }

    // 500 서버 오류 - 예상치 못한 모든 예외의 fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected (Exception e){
        log.error("예상치 못한 서버 오류", e);  //실제 원인은 로그로 기록

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("서버 오류가 발생했습니다."));  //클라이언트에는 일괄적으로 서버 오류로 내려줌
    }

    // 접근 권한이 없습니다.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(CommonErrorCode.FORBIDDEN.getMessage()));
    }



}
