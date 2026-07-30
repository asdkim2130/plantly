package project.plantly.companyTest.categoryTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import project.plantly.domain.company.category.CategoryService;
import project.plantly.domain.company.category.dto.CategoryPublicResponse;
import project.plantly.domain.company.category.tree.CategoryNode;
import project.plantly.domain.company.category.tree.CategoryTreeService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock CategoryTreeService treeService;
    @InjectMocks CategoryService categoryService;

    @Test
    @DisplayName("공개 트리는 스냅샷 루트를 중첩 DTO 로 매핑한다")
    public void getPublicTree_mapNodesToNestedDto (){
        CategoryNode root = node(1L, null, "mach", "기계", 1, true);
        CategoryNode child = node(2L, 1L, "mach-cnc", "CNC", 2, true);
        CategoryNode grandChild = node(3L, 2L, "mach-cnc-lathe", "CNC 선반", 3, true);
        addChild(root, child);
        addChild(child, grandChild);

        given(treeService.getRoots()).willReturn(List.of(root));

        List<CategoryPublicResponse> result = categoryService.getPublicTree();

        assertThat(result).hasSize(1);
        CategoryPublicResponse rootDto = result.get(0);
        assertThat(rootDto.id()).isEqualTo(1L);
        assertThat(rootDto.categoryName()).isEqualTo("기계");
        assertThat(rootDto.slug()).isEqualTo("mach");
        assertThat(rootDto.depth()).isEqualTo(1);
        assertThat(rootDto.iconUrl()).isEqualTo("icon-mach");

        // 3단(대-중-소)까지 재귀 매핑되는지 — 폼의 대→중→소 선택이 이 중첩을 그대로 쓴다.
        assertThat(rootDto.children()).hasSize(1);
        CategoryPublicResponse childDto = rootDto.children().get(0);
        assertThat(childDto.id()).isEqualTo(2L);
        assertThat(childDto.depth()).isEqualTo(2);
        assertThat(childDto.children()).hasSize(1);
        assertThat(childDto.children().get(0).id()).isEqualTo(3L);
        assertThat(childDto.children().get(0).children()).isEmpty();
    }

    @Test
    @DisplayName("비활성 루트는 활성 자식이 있어도 서브트리째 빠진다")
    public void getPublicTree_inactiveRoot_prunesSubtree (){
        CategoryNode retiredRoot = node(1L, null, "retired", "폐기된 대분류", 1, false);
        CategoryNode activeChild = node(2L, 1L, "retired-child", "살아있는 중분류", 2, true);
        addChild(retiredRoot, activeChild);

        CategoryNode aliveRoot = node(3L, null, "mach", "기계", 1, true);

        given(treeService.getRoots()).willReturn(List.of(retiredRoot, aliveRoot));

        List<CategoryPublicResponse> result = categoryService.getPublicTree();

        // 부모가 폐기되면 자식은 3단 선택에서 도달할 경로가 없다 — 루트로 승격되지도 않는다.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(3L);
        assertThat(result).flatExtracting(CategoryPublicResponse::children).isEmpty();
    }

    @Test
    @DisplayName("비활성 중분류는 자기 자신과 하위 소분류까지 빠지고 형제는 남는다")
    public void getPublicTree_inactiveChild_prunesOnlyOwnSubtree (){
        CategoryNode root = node(1L, null, "mach", "기계", 1, true);
        CategoryNode retiredChild = node(2L, 1L, "retired", "폐기된 중분류", 2, false);
        CategoryNode retiredGrandChild = node(3L, 2L, "retired-leaf", "폐기 하위 소분류", 3, true);
        CategoryNode aliveChild = node(4L, 1L, "cnc", "CNC", 2, true);
        addChild(root, retiredChild);
        addChild(retiredChild, retiredGrandChild);
        addChild(root, aliveChild);

        given(treeService.getRoots()).willReturn(List.of(root));

        List<CategoryPublicResponse> result = categoryService.getPublicTree();

        assertThat(result).hasSize(1);
        List<CategoryPublicResponse> children = result.get(0).children();
        assertThat(children).hasSize(1);
        assertThat(children.get(0).id()).isEqualTo(4L);   // 형제는 영향 없음
        assertThat(children.get(0).slug()).isEqualTo("cnc");
    }

    @Test
    @DisplayName("루트가 없으면 빈 리스트를 반환")
    public void getPublicTree_empty (){
        given(treeService.getRoots()).willReturn(List.of());

        assertThat(categoryService.getPublicTree()).isEmpty();
    }

    // 테스트 헬퍼 — CategoryAdminServiceTest 와 동일한 방식(addChild 가 패키지 전용이라 리플렉션)
    private CategoryNode node (Long id, Long parentId, String slug, String name, int depth, boolean active){
        return new CategoryNode(id, parentId, slug, name, "icon-" + slug, "desc-" + slug, depth, 0, active);
    }

    @SuppressWarnings("unchecked")
    private void addChild (CategoryNode parent, CategoryNode child){
        List<CategoryNode> children = (List<CategoryNode>) ReflectionTestUtils.getField(parent, "children");
        children.add(child);
    }
}
