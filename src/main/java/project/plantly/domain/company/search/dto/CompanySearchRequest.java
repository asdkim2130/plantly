package project.plantly.domain.company.search.dto;

import project.plantly.domain.company.search.CompanySearchCriteria;
import project.plantly.domain.company.search.CompanySearchCriteria.AdvancedText;

import java.util.List;

/**
 * {@code GET /api/v1/companies} 쿼리 파라미터 바인딩용. 평면 파라미터를 받아
 * {@link CompanySearchCriteria}(통합 keyword + 중첩 AdvancedText + 패싯 ID)로 변환한다.
 * 모든 필드 선택적 — 없으면 해당 조건이 빠진다(전체 브라우즈).
 */
public record CompanySearchRequest(

        String keyword,

        // 고급검색: 지정한 필드만 해당 컬럼에 부분일치
        String companyName,
        String introTitle,
        String content,
        String ceoName,
        String address,
        String detailAddress,
        String reference,
        String equipment,
        String material,

        // 패싯: 선택 중 하나라도 매칭(차원 내 OR). 카테고리는 후손 서브트리까지.
        List<Long> certificationIds,
        List<Long> industryIds,
        List<Long> categoryIds
) {

    public CompanySearchCriteria toCriteria() {
        return new CompanySearchCriteria(
                keyword,
                new AdvancedText(companyName, introTitle, content, ceoName,
                        address, detailAddress, reference, equipment, material),
                certificationIds, industryIds, categoryIds);
    }
}
