package project.plantly.domain.company.category.dto;

import lombok.Builder;
import project.plantly.domain.company.category.tree.CategoryNode;

import java.util.List;

@Builder
public record CategoryTreeResponse(
        Long id,
        String categoryCode,
        String categoryName,
        String iconUrl,
        String description,
        int depth,
        int displayOrder,
        boolean active,
        List<CategoryTreeResponse> children
) {
    public static CategoryTreeResponse from (CategoryNode node){
        return CategoryTreeResponse.builder()
                .id(node.getId())
                .categoryCode(node.getCategoryCode())
                .categoryName(node.getCategoryName())
                .iconUrl(node.getIconUrl())
                .description(node.getDescription())
                .depth(node.getDepth())
                .displayOrder(node.getDisplayOrder())
                .active(node.isActive())
                .children(node.getChildren().stream().map(CategoryTreeResponse::from).toList())
                .build();
    }
}
