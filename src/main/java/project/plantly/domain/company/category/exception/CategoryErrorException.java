package project.plantly.domain.company.category.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import project.plantly.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorException implements ErrorCode {



    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    CATEGORY_MAX_DEPTH(HttpStatus.BAD_REQUEST, "카테고리는 최대 3단계 까지 허용됩니다."),
    DUPLICATE_CATEGORY_CODE(HttpStatus.CONFLICT, "카테고리 코드는 중복 불가입니다."),
    PARENT_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "상위 카테고리를 찾을 수 없습니다.");



    private final HttpStatus status;
    private final String message;

}
