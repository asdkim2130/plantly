package project.plantly.companyTest.categoryTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import project.plantly.domain.company.category.Category;
import project.plantly.domain.company.category.CategoryAdminService;
import project.plantly.domain.company.category.CategoryRepository;
import project.plantly.domain.company.category.dto.CategoryCreateRequest;
import project.plantly.domain.company.category.dto.CategoryTreeResponse;
import project.plantly.domain.company.category.tree.CategoryChangedEvent;
import project.plantly.domain.company.category.tree.CategoryNode;
import project.plantly.domain.company.category.tree.CategoryTreeService;
import project.plantly.global.exception.BusinessException;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class CategoryAdminServiceTest {

    @Mock CategoryRepository categoryRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock CategoryTreeService treeService;
    @InjectMocks CategoryAdminService categoryAdminService;

    @Test
    @DisplayName("코드가 중복이면 예외가 발생하고 저장하지 않음")
    public void create_duplicateCode (){
        CategoryCreateRequest request = new CategoryCreateRequest(null, "a", "a", null, null, null);
        given(categoryRepository.existsBySlug("a")).willReturn(true);

        assertThatThrownBy(
                () -> categoryAdminService.create(request)
        ).isInstanceOf(BusinessException.class);

        verify(categoryRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("parentId가 있지만 상위 카테고리가 없으면 예외 발생")
    public void create_parentNotFound(){
        CategoryCreateRequest request = new CategoryCreateRequest(99L, "a", "a", null, null, null);
        given(categoryRepository.existsBySlug("a")).willReturn(false);
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(
                () -> categoryAdminService.create(request)
        ).isInstanceOf(BusinessException.class);

    }

    @Test
    @DisplayName("displayOrder 미입력 시 형제 최대값 +1로 저장하고 커밋 후 이벤트 발행")
    public void create_autoDisplayOrder_andPublishEvent(){
        CategoryCreateRequest request = new CategoryCreateRequest(null, "a", "a", null, null, null);

        given(categoryRepository.existsBySlug("a")).willReturn(false);
        given(categoryRepository.findMaxDisplayOrderByParentId(null)).willReturn(2);
        given(categoryRepository.save(any(Category.class))).willAnswer(
                inv -> {
                    Category c = inv.getArgument(0);
                    ReflectionTestUtils.setField(c, "id", 1L);
                    return c;
                }
        );

        Long id = categoryAdminService.create(request);

        assertThat(id).isEqualTo(1L);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(3);

        verify(eventPublisher).publishEvent(any(CategoryChangedEvent.class));
    }

    @Test
    @DisplayName("카테고리 트리 구조 캐시 루트 노드들을 중첩 DTO 트리로 반환")
    public void getTree_mapNodesToNestedDto (){

        CategoryNode root = node(1L, null, "a", "a", 1, 0, true);
        CategoryNode child = node(2L, 1L, "a-1", "a-1", 2, 0, true);
        addChild(root, child);

        given(treeService.getRoots()).willReturn(List.of(root));

        List<CategoryTreeResponse> result = categoryAdminService.getTree();

        // 루트 매핑 검증
        assertThat(result).hasSize(1);
        CategoryTreeResponse rootDto = result.get(0);
        assertThat(rootDto.id()).isEqualTo(1L);
        assertThat(rootDto.slug()).isEqualTo("a");
        assertThat(rootDto.depth()).isEqualTo(1);
        assertThat(rootDto.active()).isTrue();

        // 중첩 칠드런 재귀 매핑 검증
        assertThat(rootDto.children()).hasSize(1);
        CategoryTreeResponse childDto = rootDto.children().get(0);
        assertThat(childDto.id()).isEqualTo(2L);
        assertThat(childDto.categoryName()).isEqualTo("a-1");
        assertThat(childDto.children()).isEmpty();

    }

    @Test
    @DisplayName("관리자 트리는 비활성 노드를 서브트리째 유지한다 (공개 트리와 조회 범위가 다르다)")
    public void getTree_includesInactive (){

        CategoryNode retiredRoot = node(1L, null, "retired", "폐기된 대분류", 1, 0, false);
        CategoryNode childOfRetired = node(2L, 1L, "retired-child", "살아있는 중분류", 2, 0, true);
        addChild(retiredRoot, childOfRetired);

        CategoryNode aliveRoot = node(3L, null, "mach", "기계", 1, 1, true);
        CategoryNode retiredChild = node(4L, 3L, "retired-cnc", "폐기된 중분류", 2, 0, false);
        addChild(aliveRoot, retiredChild);

        given(treeService.getRoots()).willReturn(List.of(retiredRoot, aliveRoot));

        List<CategoryTreeResponse> result = categoryAdminService.getTree();

        // 운영자는 폐기한 카테고리도 보고 되살릴 수 있어야 한다. 공개 트리
        // (CategoryPublicResponse.activeTreeOf)처럼 비활성 서브트리를 쳐내면 안 된다 —
        // 두 경로가 같은 스냅샷을 각자 DTO 로 매핑하므로 필터를 잘못 옮겨오기 쉽다.
        assertThat(result).hasSize(2);

        CategoryTreeResponse retiredRootDto = result.get(0);
        assertThat(retiredRootDto.id()).isEqualTo(1L);
        assertThat(retiredRootDto.active()).isFalse();
        assertThat(retiredRootDto.children()).hasSize(1);   // 폐기 부모 밑의 자식도 남는다
        assertThat(retiredRootDto.children().get(0).id()).isEqualTo(2L);

        // 활성 부모 밑의 비활성 자식도 active=false 로 그대로 내려간다(운영 화면의 회색 표시용).
        CategoryTreeResponse aliveRootDto = result.get(1);
        assertThat(aliveRootDto.active()).isTrue();
        assertThat(aliveRootDto.children()).hasSize(1);
        assertThat(aliveRootDto.children().get(0).id()).isEqualTo(4L);
        assertThat(aliveRootDto.children().get(0).active()).isFalse();
    }

    @Test
    @DisplayName("루트가 없으면 빈 리스트를 반환")
    public void getTree_empty(){
        given(treeService.getRoots()).willReturn(List.of());

        assertThat(categoryAdminService.getTree()).isEmpty();
    }



    // 테스트 헬퍼
    private CategoryNode node (Long id, Long parentId, String code, String name, int depth, int order, boolean active){
        return new CategoryNode(id, parentId, code, name, "icon-"+code, "desc-" + code, depth, order, active);
    }

    @SuppressWarnings("unchecked")
    private void addChild (CategoryNode parent, CategoryNode child){
        List<CategoryNode> children = (List<CategoryNode>) ReflectionTestUtils.getField(parent, "children");
        children.add(child);
    }
}
