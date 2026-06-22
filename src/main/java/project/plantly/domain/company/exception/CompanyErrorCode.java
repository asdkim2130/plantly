package project.plantly.domain.company.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import project.plantly.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum CompanyErrorCode implements ErrorCode {

    // 등급별 카테고리 최대 저장 개수를 초과한 경우
    CATEGORY_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "현재 등급에서 선택 가능한 카테고리 개수를 초과했습니다."),

    // 동영상(videoUrl) 사용이 허용되지 않는 등급인 경우
    VIDEO_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "현재 등급에서는 동영상을 등록할 수 없습니다."),

    // 레퍼런스 이미지 업로드가 허용되지 않는 등급인 경우
    REFERENCE_IMAGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "현재 등급에서는 레퍼런스 이미지를 등록할 수 없습니다."),

    // 레퍼런스 이미지가 등급별 최대 장수를 초과한 경우
    REFERENCE_IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "현재 등급에서 등록 가능한 레퍼런스 이미지 장수를 초과했습니다."),

    // 링크(M:N) 등록 시 요청에 존재하지 않는 마스터 ID가 포함된 경우
    CATEGORY_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 카테고리가 포함되어 있습니다."),
    CERTIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 인증이 포함되어 있습니다."),
    COUNTRY_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 국가가 포함되어 있습니다."),
    DOMESTIC_REGION_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 국내 지역이 포함되어 있습니다."),
    INDUSTRY_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 산업군이 포함되어 있습니다.");

    private final HttpStatus status;
    private final String message;
}
