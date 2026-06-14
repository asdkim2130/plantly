package project.plantly.domain.company.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CategoryCreateRequest (
        Long parentId,
        @NotBlank
        String categoryName,
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9-]+$",
                message = "카테고리 코드는 영문, 숫자, 하이픈(-)만 입력할 수 있습니다.")
        String categoryCode,
        @Pattern(regexp = "^(https?://)?[A-Za-z0-9._~:/?#@!$&'()*+,;=%\\[\\]-]+$",
                message = "아이콘 URL은 한글 없이 http/https URL 또는 상대 경로 형식이어야 합니다.")
        String iconUrl,
        String description,
        Integer displayOrder
){
}
