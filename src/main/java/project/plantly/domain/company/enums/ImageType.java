package project.plantly.domain.company.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageType {

    MAIN("대표 이미지"),
    PORTFOLIO("포트폴리오"),
    FACILITY("설비/시설");      // ← 아래 노트 참고
    private final String label;
}
