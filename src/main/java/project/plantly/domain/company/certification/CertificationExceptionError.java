package project.plantly.domain.company.certification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import project.plantly.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum CertificationExceptionError implements ErrorCode {

    DUPLICATE_CERTIFICATION_NAME(HttpStatus.CONFLICT, "인증 이름은 중복 불가합니다.");

    private final HttpStatus status;
    private final String message;
}
