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
        given(categoryRepository.existsByCategoryCode("a")).willReturn(true);

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
        given(categoryRepository.existsByCategoryCode("a")).willReturn(false);
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(
                () -> categoryAdminService.create(request)
        ).isInstanceOf(BusinessException.class);

    }

    @Test
    @DisplayName("displayOrder 미입력 시 형제 최대값 +1로 저장하고 커밋 후 이벤트 발행")
    public void create_autoDisplayOrder_andPublishEvent(){
        CategoryCreateRequest request = new CategoryCreateRequest(null, "a", "a", null, null, null);

        given(categoryRepository.existsByCategoryCode("a")).willReturn(false);
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

        CategoryNode root = node(1L, null, "a", "a", 1, 0);
        CategoryNode child = node(2L, 1L, "a-1", "a-1", 2, 0);
        addChild(root, child);

        given(treeService.getRoots()).willReturn(List.of(root));

        List<CategoryTreeResponse> result = categoryAdminService.getTree();

        // 루트 매핑 검증
        assertThat(result).hasSize(1);
        CategoryTreeResponse rootDto = result.get(0);
        assertThat(rootDto.id()).isEqualTo(1L);
        assertThat(rootDto.categoryCode()).isEqualTo("a");
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
    @DisplayName("루트가 없으면 빈 리스트를 반환")
    public void getTree_empty(){
        given(treeService.getRoots()).willReturn(List.of());

        assertThat(categoryAdminService.getTree()).isEmpty();
    }



    // 테스트 헬퍼
    private CategoryNode node (Long id, Long parentId, String code, String name, int depth, int order){
        return new CategoryNode(id, parentId, code, name, "icon-"+code, "desc-" + code, depth, order, true);
    }

    @SuppressWarnings("unchecked")
    private void addChild (CategoryNode parent, CategoryNode child){
        List<CategoryNode> children = (List<CategoryNode>) ReflectionTestUtils.getField(parent, "children");
        children.add(child);
    }
}
