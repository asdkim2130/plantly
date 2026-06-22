package project.plantly.domain.company.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PricingType {
    FIXED("고정 단가제"),
    CONSULTATION("상담 후 결정"),
    PROJECT_BASED("프로젝트별 상이");

    private final String label;
}
