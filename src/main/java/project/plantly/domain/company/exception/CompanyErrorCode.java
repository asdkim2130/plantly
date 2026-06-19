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

    // 링크(M:N) 등록 시 요청에 존재하지 않는 마스터 ID가 포함된 경우
    CATEGORY_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 카테고리가 포함되어 있습니다."),
    CERTIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 인증이 포함되어 있습니다."),
    COUNTRY_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 국가가 포함되어 있습니다."),
    DOMESTIC_REGION_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 국내 지역이 포함되어 있습니다."),
    INDUSTRY_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 산업군이 포함되어 있습니다.");

    private final HttpStatus status;
    private final String message;
}
