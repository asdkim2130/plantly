package project.plantly.global.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
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
