package project.plantly.domain.company.industry.dto;

import lombok.Builder;
import project.plantly.domain.company.industry.Industry;

@Builder
public record IndustryAdminResponse(
        Long id,
        String industryName,
        String slug,
        String iconUrl,
        String description,
        int displayOrder,
        boolean active
) {

    public static IndustryAdminResponse from (Industry industry){
        return IndustryAdminResponse.builder()
                .id(industry.getId())
                .industryName(industry.getIndustryName())
                .slug(industry.getSlug())
                .iconUrl(industry.getIconUrl())
                .description(industry.getDescription())
                .displayOrder(industry.getDisplayOrder())
                .active(industry.isActive())
                .build();
    }
}
