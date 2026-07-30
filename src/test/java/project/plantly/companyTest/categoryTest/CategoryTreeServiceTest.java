package project.plantly.companyTest.categoryTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.test.util.ReflectionTestUtils;
import project.plantly.domain.company.category.Category;
import project.plantly.domain.company.category.CategoryRepository;
import project.plantly.domain.company.category.tree.CategoryNode;
import project.plantly.domain.company.category.tree.CategoryTreeService;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class CategoryTreeServiceTest {

    @Mock CategoryRepository categoryRepository;
    @InjectMocks CategoryTreeService treeService;

    @Test
    @DisplayName("빈 DB 로 기동해도 시드 적재 후 스냅샷이 재빌드된다")
    public void reloadAfterStartup_picksUpSeededRows (){
        Category root = Category.createRoot("기계", "mach", null, null, 0);
        ReflectionTestUtils.setField(root, "id", 1L);

        given(categoryRepository.findAllByOrderByDisplayOrderAsc())
                .willReturn(List.of())        // 1회차: @PostConstruct 시점 — 시드 전이라 0행
                .willReturn(List.of(root));   // 2회차: ApplicationRunner 시드 적재 후

        treeService.reload();  // @PostConstruct init() 이 하는 일
        assertThat(treeService.getRoots()).isEmpty();

        // 기동 완료 후 재빌드가 없으면 카테고리 조회가 계속 빈 배열을 반환한다.
        ReflectionTestUtils.invokeMethod(treeService, "reloadAfterStartup");

        assertThat(treeService.getRoots()).extracting(CategoryNode::getId).containsExactly(1L);
    }

    @Test
    @DisplayName("재빌드 훅은 ApplicationReadyEvent 에 걸려 있어야 한다 (시더보다 늦게 도는 유일한 시점)")
    public void reloadAfterStartup_wiredToApplicationReadyEvent () throws Exception {
        Method hook = CategoryTreeService.class.getDeclaredMethod("reloadAfterStartup");
        EventListener listener = hook.getAnnotation(EventListener.class);

        // ContextRefreshedEvent 등 더 이른 이벤트로 바꾸면 시드보다 먼저 돌아 버그가 되살아난다.
        assertThat(listener).isNotNull();
        assertThat(listener.value()).containsExactly(ApplicationReadyEvent.class);
    }
}
