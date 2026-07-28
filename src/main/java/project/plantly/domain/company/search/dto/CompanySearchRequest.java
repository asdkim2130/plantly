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
        // 인증만 예외 — type(경영시스템/산업특화/시장진입) 내부는 OR, type 간에는 AND.
        // 프론트가 type 별 드롭다운을 분리하므로 각 드롭다운이 독립 조건으로 동작한다.
        // 평면 리스트로 받고 서버가 type 으로 묶는다(프론트가 그룹 구조를 실어 보낼 필요 없음).
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
