package project.plantly.domain.company.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// CompanyImage 의 분류. 단일 대표 이미지(logo)는 Company 스칼라라 여기 포함하지 않는다.
// 필요 시 회사 갤러리 세부 종류(포트폴리오/설비 등)를 DETAIL 자리에 추가 분화할 수 있다.
@Getter
@RequiredArgsConstructor
public enum ImageType {

    DETAIL("상세 이미지"),     // 회사 직속 갤러리
    PROJECT("프로젝트 이미지");  // 프로젝트 레퍼런스 소속

    private final String label;
}