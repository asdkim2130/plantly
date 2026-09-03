package project.plantly.domain.upload.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import project.plantly.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum UploadErrorCode implements ErrorCode {

    // 파트가 아예 없거나 빈 파일인 경우. @RequestPart 를 required=false 로 받아 이 코드로 모은다
    // (required=true 면 MissingServletRequestPartException 이 fallback 핸들러에 걸려 500 으로 나간다).
    FILE_REQUIRED(HttpStatus.BAD_REQUEST, "업로드할 파일이 없습니다."),

    // 매직 바이트 판별 결과가 허용 형식(JPEG/PNG/WebP)이 아닌 경우.
    // 사용자가 무엇을 해야 하는지 알 수 있게 허용 목록을 문장에 넣는다.
    UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "JPG, PNG, WebP 이미지만 업로드할 수 있습니다."),

    // 존재하지 않는 key. 형태가 틀린 key 도 같은 응답으로 묶는다 — 그런 파일은 존재할 수 없으므로
    // "잘못된 형식"과 "없음"을 가르는 것은 클라이언트에게 아무 정보도 주지 않는다.
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 파일입니다."),

    // 디스크 쓰기/읽기 실패. 사용자 입력 문제가 아니므로 5xx 로 낸다.
    STORAGE_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
