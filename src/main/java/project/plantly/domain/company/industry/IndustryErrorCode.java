package project.plantly.domain.company.industry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import project.plantly.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum IndustryErrorCode implements ErrorCode {

    DUPLICATE_INDUSTRY_CODE(HttpStatus.CONFLICT, "산업군 코드는 중복 불가합니다."),
    DUPLICATE_INDUSTRY_NAME(HttpStatus.CONFLICT, "산업군 이름은 중복 불가합니다.");

    private final HttpStatus status;
    private final String message;
}
