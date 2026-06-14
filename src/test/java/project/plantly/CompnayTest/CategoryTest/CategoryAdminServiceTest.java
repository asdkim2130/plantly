package project.plantly.CompnayTest.CategoryTest;

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
import project.plantly.domain.company.category.tree.CategoryChangedEvent;
import project.plantly.global.exception.BusinessException;

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
}
