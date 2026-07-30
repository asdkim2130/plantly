package project.plantly.domain.company.certification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import project.plantly.domain.company.certification.CertificationType;

public record CertificationCreateRequest(
        @NotBlank
        String name,
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9-]+$",
                message = "슬러그는 영문, 숫자, 하이픈(-)만 입력할 수 있습니다.")
        String slug,
        @NotNull(message = "인증 구분은 필수입니다.")
        CertificationType type,
        @PositiveOrZero(message = "displayOrder는 0 이상이어야 합니다.")
        Integer displayOrder) {
}
