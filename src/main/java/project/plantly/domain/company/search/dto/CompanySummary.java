package project.plantly.domain.company.search.dto;

/**
 * 회사 검색 결과 1건(리스트 카드). 조인 없는 Company 스칼라만 구성한다 —
 * 산업군/카테고리 칩, 레퍼런스 썸네일 등 조인 필요한 항목은 추후 확장.
 */
public record CompanySummary(
        Long id,
        String companyName,
        String introTitle,
        String logoUrl,
        String address,
        boolean verified,
        boolean featured,
        boolean spotlight
) {}
