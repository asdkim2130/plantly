package project.plantly.domain.company.category;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.plantly.domain.company.category.exception.CategoryErrorException;
import project.plantly.global.exception.BusinessException;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Category extends CompanyChild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentId;

    @NotNull
    @Column(nullable = false)
    private String categoryName;

    @NotNull
    @Column(nullable = false, unique = true)
    private String categoryCode;

    private String iconUrl;
    private String description;

    @Column(nullable = false)
    private int depth;

    public Category(Long parentId, String categoryName, String categoryCode, String iconUrl, String description, int depth) {
        this.parentId = parentId;
        this.categoryName = categoryName;
        this.categoryCode = categoryCode;
        this.iconUrl = iconUrl;
        this.description = description;
        this.depth = depth;
    }

    //대분류 생성
    public static Category createRoot (String name, String code, String iconUrl, String description, int displayOrder){
        return create(null, name, code, iconUrl, description, 1, displayOrder);
    }

    public static Category createChild (Category parent, String name, String code, String iconUrl, String description, int displayOrder){
        if(parent.getDepth() >=3 ){
            throw new BusinessException(CategoryErrorException.CATEGORY_MAX_DEPTH);
        }
        return create(parent.getId(), name, code, iconUrl, description, parent.getDepth()+1, displayOrder);
    }

    private static Category create (Long parentId, String name, String code, String iconUrl, String description, int depth, int displayOrder){
        Category category = new Category(parentId, name, code, iconUrl, description, depth);
        category.activate();                       // 생성 시 기본 활성
        category.changeDisplayOrder(displayOrder); // 형제 정렬 순서 부여
        return category;
    }

    public void updateInfo (String name, String iconUrl, String description, int displayOrder){
        this.categoryName = name;
        this.iconUrl = iconUrl;
        this.description = description;
        changeDisplayOrder(displayOrder);
    }

    public void changeActive (boolean active){
        if (active) activate();
        else deactivate();
    }
}

