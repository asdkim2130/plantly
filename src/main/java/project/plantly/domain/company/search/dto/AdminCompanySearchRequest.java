package project.plantly.domain.company.search.dto;

import project.plantly.domain.company.search.AdminCompanySearchCriteria;

/**
 * {@code GET /api/v1/admin/companies} 쿼리 파라미터 바인딩용. 평면 파라미터를 받아
 * {@link AdminCompanySearchCriteria} 로 변환한다. 모든 필드 선택적 — 없으면 해당 조건이 빠진다(전체 목록).
 *
 * <p>verified/featured/spotlight/deleted 는 박싱 {@code Boolean} 이라 '파라미터 누락 → null(상관없음)' 과
 * '=false → false(거짓만)' 가 구분된다(원시 boolean 이면 누락이 false 로 뭉개져 3-상태가 깨진다).
 */
public record AdminCompanySearchRequest(
        Boolean verified,
        Boolean featured,
        Boolean spotlight,
        Boolean deleted,
        String companyName,
        Long ownerUserId
) {

    public AdminCompanySearchCriteria toCriteria() {
        return new AdminCompanySearchCriteria(
                verified, featured, spotlight, deleted, companyName, ownerUserId);
    }
}
