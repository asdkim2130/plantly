package project.plantly.domain.company.certification.dto;

import lombok.Builder;
import project.plantly.domain.company.certification.Certification;
import project.plantly.domain.company.certification.CertificationType;

@Builder
public record CertificationAdminResponse(
        Long id,
        String certificationName,
        String slug,
        CertificationType type,
        int displayOrder,
        boolean active

) {

    public static CertificationAdminResponse from (Certification certification){
        return CertificationAdminResponse.builder()
                .id(certification.getId())
                .certificationName(certification.getCertificationName())
                .slug(certification.getSlug())
                .type(certification.getType())
                .displayOrder(certification.getDisplayOrder())
                .active(certification.isActive())
                .build();
    }
}
