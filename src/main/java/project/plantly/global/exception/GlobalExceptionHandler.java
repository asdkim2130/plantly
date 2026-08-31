package project.plantly.global.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import project.plantly.global.response.ApiResponse;

import java.util.Objects;

// ResponseEntityExceptionHandler 를 상속한다.
//
// 이 advice 의 @ExceptionHandler(Exception.class) fallback 은 스프링의 기본 처리
// (DefaultHandlerExceptionResolver)보다 먼저 잡는다 - ExceptionHandlerExceptionResolver 의 우선순위가
// 더 높기 때문이다. 그래서 스프링이 알아서 4xx 로 매핑해 줄 표준 MVC 예외까지 전부 500 으로 나갔다.
// 실제로 깨진 JSON·타입 불일치·필수 파라미터 누락·필수 파트 누락·잘못된 메서드가 400/405 대신 500 이었고,
// 매핑되지 않은 URL(NoResourceFoundException)까지 500 이라 오타 난 주소가 "서버 장애"로 읽혔다.
// 하나씩 핸들러를 다는 방식은 또 새는 곳을 남기므로, 표준 예외 20여 종의 상태 코드 매핑을 부모에게 넘긴다.
//
// 다만 응답 본문은 기존 ApiResponse 봉투를 유지한다. 부모의 기본 본문은 ProblemDetail 이지만,
// 상태 코드 교정(상속의 목적)과 본문 형식은 분리할 수 있다 - createResponseEntity 한 곳에서 갈아끼운다.
// ProblemDetail 로 전면 전환하지 않는 이유는 비용이 이 클래스 밖에 있기 때문이다: 시큐리티 필터단의
// 401/403(SecurityResponseWriter)은 MVC 밖이라 이 상속이 닿지 않아 형식이 갈리고, 프론트는 별도 repo 라
// 락스텝 배포가 필요하다.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 409, 500 중 의도된 비즈니스 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e){
        ErrorCode code = e.getErrorCode();

        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.failure(code.getMessage()));
    }

    // 접근 권한이 없습니다.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(CommonErrorCode.FORBIDDEN.getMessage()));
    }

    // 500 서버 오류 - 부모가 다루지 않는, 예상치 못한 모든 예외의 fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected (Exception e){
        log.error("예상치 못한 서버 오류", e);  //실제 원인은 로그로 기록

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("서버 오류가 발생했습니다."));  //클라이언트에는 일괄적으로 서버 오류로 내려줌
    }

    // --- 아래는 부모가 이미 @ExceptionHandler 로 잡는 예외들의 처리 지점(protected 훅) ---
    // 같은 예외 타입에 @ExceptionHandler 를 새로 달면 부모 것과 겹쳐 기동 시 ambiguous mapping 으로 터진다.
    // 훅은 어노테이션이 없으므로 오버라이드로만 손댄다.

    // 400 검증 실패 - 요청 본문 객체 바인딩·검증
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        // 필드 에러가 하나도 없는 경우(클래스 레벨 제약만 걸린 경우)가 있어 첫 원소를 바로 꺼내지 않는다.
        String firstMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(CommonErrorCode.INVALID_INPUT.getMessage());

        return handleExceptionInternal(ex, ApiResponse.failure(firstMessage), headers, status, request);
    }

    // 400 검증 실패 - 메서드 파라미터 검증. 위 MethodArgumentNotValidException 은 "요청 본문 객체"를 바인딩·검증할 때
    // 나오고, 이쪽은 본문이 객체가 아니라 리스트라 파라미터 레벨 검증이 도는 경우(@Valid 의 원소 중첩 검증,
    // List<@NotNull X> 같은 컨테이너 원소 제약)에 나온다.
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String firstMessage = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(CommonErrorCode.INVALID_INPUT.getMessage());

        return handleExceptionInternal(ex, ApiResponse.failure(firstMessage), headers, status, request);
    }

    // 400 업로드 용량 초과 - 서블릿 컨테이너가 요청을 다 받기 전에 끊고 던진다. 컨트롤러에 닿지 않으므로
    // UploadService 의 상한 검사로는 잡히지 않는다. 부모는 이걸 413 으로 주지만, "입력 오류는 400" 이라는
    // 이 API 의 규칙에 맞춰 400 으로 되돌린다(CommonErrorCode.FILE_TOO_LARGE 주석 참고).
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        return handleExceptionInternal(ex, ApiResponse.failure(CommonErrorCode.FILE_TOO_LARGE.getMessage()),
                headers, CommonErrorCode.FILE_TOO_LARGE.getStatus(), request);
    }

    // 표준 예외가 지나가는 단일 통로. 진단값(받은 Content-Type, 변환 실패한 값 등)은 예외 메시지에 들어 있으므로
    // 여기서 로그로만 남기고 클라이언트에게는 일반화한 문구를 내려보낸다 - 사용자가 화면에서 만들 수 있는
    // 상태가 아니라 클라이언트 구현 오류다.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        if (statusCode.is5xxServerError()) {
            log.error("요청 처리 중 서버 오류 (status={})", statusCode.value(), ex);
        } else {
            log.warn("거절된 요청 (status={}): {}: {}", statusCode.value(), ex.getClass().getSimpleName(), ex.getMessage());
        }

        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    // 부모가 만든 ProblemDetail 본문을 ApiResponse 봉투로 갈아끼우는 지점. 위 훅들이 이미 봉투를 만들어
    // 넘긴 경우에는 그대로 둔다.
    //
    // 참고: Accept 헤더가 JSON 이 아니어서 406 이 되는 경우에는 이 봉투도 쓰이지 못하고 본문 없이 406 만 나간다 -
    // 클라이언트가 받지 않겠다고 선언한 형식으로 봉투를 실어 보낼 수는 없다.
    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        if (body instanceof ApiResponse) {
            return new ResponseEntity<>(body, headers, statusCode);
        }

        return new ResponseEntity<>(ApiResponse.failure(defaultMessage(statusCode)), headers, statusCode);
    }

    // 스프링이 요청 자체를 거절한 경우의 문구. 예외 종류가 아니라 상태 코드로 고른다 -
    // 부모가 다루는 예외가 20여 종이라 종류마다 문구를 두면 다시 "빠뜨린 예외" 문제가 생기고,
    // 클라이언트에게 유의미한 구분은 어차피 상태 코드 단위다.
    private String defaultMessage(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());

        if (status == null) {
            return CommonErrorCode.INTERNAL_ERROR.getMessage();
        }

        return switch (status) {
            case NOT_FOUND -> CommonErrorCode.NOT_FOUND.getMessage();
            case METHOD_NOT_ALLOWED -> CommonErrorCode.METHOD_NOT_ALLOWED.getMessage();
            case NOT_ACCEPTABLE -> CommonErrorCode.NOT_ACCEPTABLE.getMessage();
            case UNSUPPORTED_MEDIA_TYPE -> CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage();
            case PAYLOAD_TOO_LARGE -> CommonErrorCode.FILE_TOO_LARGE.getMessage();
            default -> statusCode.is4xxClientError()
                    ? CommonErrorCode.INVALID_INPUT.getMessage()
                    : CommonErrorCode.INTERNAL_ERROR.getMessage();
        };
    }
}
