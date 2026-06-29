package project.plantly.domain.company.search.dto;

import java.util.List;

/**
 * 회사 검색 결과 1건(리스트 카드). Company 스칼라 + 회사가 연결한 카테고리/태그/산업군 이름 목록.
 * 이름 목록은 검색 시점에 링크 테이블에서 집계한다(카테고리는 회사가 직접 고른 것만 — closure 조상은 제외).
 */
public record CompanySummary(
        Long id,
        String companyName,
        String introTitle,
        String logoUrl,
        String address,
        boolean verified,
        boolean featured,
        boolean spotlight,
        List<String> categoryNames,
        List<String> tagNames,
        List<String> industryNames
) {}
