package project.plantly.domain.company.certification.dto;

import lombok.Builder;
import project.plantly.domain.company.certification.Certification;

@Builder
public record CertificationAdminResponse(
        Long id,
        String certificationName,
        int displayOrder,
        boolean active

) {

    public static CertificationAdminResponse from (Certification certification){
        return CertificationAdminResponse.builder()
                .id(certification.getId())
                .certificationName(certification.getCertificationName())
                .displayOrder(certification.getDisplayOrder())
                .active(certification.isActive())
                .build();
    }
}
