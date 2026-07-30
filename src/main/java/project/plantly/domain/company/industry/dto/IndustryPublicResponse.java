package project.plantly.domain.company.industry.dto;

import project.plantly.domain.company.industry.Industry;

/**
 * 공개 산업 옵션. 회사 등록 폼과 검색 필터 패널이 드롭다운을 그리는 데 쓴다.
 *
 * <p>운영 필드(displayOrder/active)는 노출하지 않는다 — 정렬은 서버가 이미 적용했고,
 * 비활성 항목은 애초에 목록에서 빠지므로 클라이언트가 알 필요가 없다.
 * description 도 뺀다: 시드가 값을 넣지 않아 전량 null 이고 드롭다운 라벨에도 쓰이지 않는다.
 * 툴팁 등으로 필요해지면 필드 추가만 하면 되므로 비파괴적으로 되돌릴 수 있다.
 * ({@code CertificationPublicResponse} 와 동일한 기준)
 */
public record IndustryPublicResponse(
        Long id,
        String industryName,
        String slug,
        String iconUrl
) {

    public static IndustryPublicResponse from(Industry industry) {
        return new IndustryPublicResponse(
                industry.getId(),
                industry.getIndustryName(),
                industry.getSlug(),
                industry.getIconUrl());
    }
}
