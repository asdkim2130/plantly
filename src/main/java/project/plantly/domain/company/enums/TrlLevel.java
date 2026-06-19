package project.plantly.domain.company.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrlLevel {
    PROTOTYPE("프로토타입"),
    MASS_PRODUCTION("양산 적용 가능"),
    GLOBAL_STANDARD("글로벌 표준");

    private final String label;

}
