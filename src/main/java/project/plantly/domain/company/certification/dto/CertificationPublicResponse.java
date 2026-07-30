package project.plantly.domain.company.certification.dto;

import project.plantly.domain.company.certification.Certification;
import project.plantly.domain.company.certification.CertificationType;

/**
 * 공개 인증 옵션. 회사 등록 폼과 검색 필터 패널이 드롭다운을 그리는 데 쓴다.
 *
 * <p>평면 목록으로 내려주고 그룹핑은 프론트가 {@code type} 으로 한다. 서버가 미리 묶어 내려주지 않는 이유는
 * 그룹 라벨("경영 지원 인증" 등)·배지 색·그룹 노출 순서가 모두 표현 계층 관심사이기 때문이다.
 * 다른 참조데이터 응답도 평면이며 트리는 지역(DomesticRegion)뿐이다.
 *
 * <p>운영 필드(displayOrder/active)는 노출하지 않는다 — 정렬은 서버가 이미 적용했고,
 * 비활성 항목은 애초에 목록에서 빠지므로 클라이언트가 알 필요가 없다.
 */
public record CertificationPublicResponse(
        Long id,
        String certificationName,
        String slug,
        CertificationType type
) {

    public static CertificationPublicResponse from(Certification certification) {
        return new CertificationPublicResponse(
                certification.getId(),
                certification.getCertificationName(),
                certification.getSlug(),
                certification.getType());
    }
}
