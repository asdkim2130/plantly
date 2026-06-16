package project.plantly.domain.company.certification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CertificationCreateRequest(
        @NotBlank
        String name,
        @PositiveOrZero(message = "displayOrder는 0 이상이어야 합니다.")
        Integer displayOrder) {
}
