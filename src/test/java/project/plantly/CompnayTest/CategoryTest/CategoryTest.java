package project.plantly.CompnayTest.CategoryTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import project.plantly.domain.company.category.Category;
import project.plantly.global.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CategoryTest {

    @Test
    @DisplayName("대분류 생성 - parentId = null, depth =1 이 기본으로 활성화")
    public void createRoot (){
        Category root = Category.createRoot("제조 인프라", "manufacture-infra", null, "설명", 0);

        assertThat(root.getParentId()).isNull();
        assertThat(root.getDepth()).isEqualTo(1);
        assertThat(root.isActive()).isTrue();
        assertThat(root.getDisplayOrder()).isEqualTo(0);
//        assertThat(root.getCreatedAt()).isNotNull();
//        assertThat(root.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("child 생성 시 parentId는 부모 id, depth는 부모 + 1")
    public void createChild(){
        Category parent = Category.createRoot("제조 인프라", "manufacture-infra", null, "설명", 0);
        ReflectionTestUtils.setField(parent, "id", 10L);

        Category child = Category.createChild(parent, "생산/공정관리", "maintain", null, "생산/공정관리 설명", 0);

        assertThat(child.getParentId()).isEqualTo(10L);
        assertThat(child.getDepth()).isEqualTo(2);
    }

    @Test
    @DisplayName("depth = 3인 부모에 자식을 만들면 최대 깊이 예외 발생")
    public void createdChild_exceedMaxDepth(){
        Category depth3 = Category.createRoot("a", "a", null, null, 0);
        ReflectionTestUtils.setField(depth3, "depth", 3);

        assertThatThrownBy(
                () -> Category.createChild(depth3, "b", "b", null, null, 0)
        ).isInstanceOf(BusinessException.class);

    }
}
