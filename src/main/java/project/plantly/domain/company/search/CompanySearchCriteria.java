package project.plantly.domain.company.search;

import java.util.List;

/**
 * 회사 검색 질의 계약. PG(현재)·ES(차후) 공통 입력.
 *
 * <p>text(query)와 facet(filter)을 분리한다 — ES 의 query/filter context 와 1:1.
 * 정렬은 v1 단일 기본정렬(spotlight→featured→최신 createdAt desc)이라 구현에 고정돼 있어,
 * 선택지가 없으므로 계약에 두지 않는다(거짓 계약 회피). 정렬 옵션이 생기면 그때 필드를 추가한다.
 */
public record CompanySearchCriteria(

        // 통합검색 박스. null/blank 이면 키워드 조건 없음(전체 브라우즈) → search_all ILIKE '%keyword%'.
        String keyword,

        // 고급검색: 지정된 필드만 부분일치. 각 필드 null 이면 그 필드 조건 없음.
        AdvancedText advanced,

        // 패싯 필터(AND, exact ID). 빈/null 리스트면 해당 패싯 필터 없음.
        List<Long> certificationIds,
        List<Long> industryIds,
        // 카테고리는 계층이라 선택 id + 모든 후손을 closure 로 매칭(서브트리).
        List<Long> categoryIds
) {

    /**
     * 고급검색 필드. 검색 가능한 도큐먼트 컬럼(스칼라 6 + 1:N 집계 3)과 1:1.
     * reference=projectTitle/achievements/partners, equipment=equipmentName, material=materialName.
     */
    public record AdvancedText(
            String companyName,
            String introTitle,
            String content,
            String ceoName,
            String address,
            String detailAddress,
            String reference,
            String equipment,
            String material
    ) {}
}
