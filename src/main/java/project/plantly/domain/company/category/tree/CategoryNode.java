package project.plantly.domain.company.category.tree;

import lombok.*;
import project.plantly.domain.company.category.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public class CategoryNode {

    private final Long id;
    private final Long parentId;
    private final String categoryCode;
    private final String categoryName;
    private final String iconUrl;
    private final String description;
    private final int depth;
    private final int displayOrder;
    private final boolean active;
    private final List<CategoryNode> children = new ArrayList<>();

    public CategoryNode (Category category){
        this.id = category.getId();
        this.parentId = category.getParentId();
        this.categoryCode = category.getCategoryCode();
        this.categoryName = category.getCategoryName();
        this.iconUrl = category.getIconUrl();
        this.description = category.getDescription();
        this.depth = category.getDepth();
        this.displayOrder = category.getDisplayOrder();
        this.active = category.isActive();
    }

    // 빌드시에만 사용
    void addChild (CategoryNode child){
        children.add(child);
    }

    // 빌드 후에는 불변
    public List<CategoryNode> getChildren (){
        return Collections.unmodifiableList(children);
    }

}
