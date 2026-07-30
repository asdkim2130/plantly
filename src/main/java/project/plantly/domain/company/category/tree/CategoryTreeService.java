package project.plantly.domain.company.category.tree;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import project.plantly.domain.company.category.Category;
import project.plantly.domain.company.category.exception.CategoryErrorException;
import project.plantly.domain.company.category.CategoryRepository;
import project.plantly.global.exception.BusinessException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CategoryTreeService {

    private final CategoryRepository categoryRepository;
    private volatile CategorySnapshot categorySnapshot;  // 원자적 교체 대상

    // 기동 시 1회 빌드. 스냅샷 non-null 을 보장하는 것이 주 목적 — 웹 서버는 컨텍스트 refresh
    // 중에 이미 뜨므로, 이 시점에 채워두지 않으면 getRoots() 가 NPE 를 던질 창이 생긴다.
    @PostConstruct
    void init() {
        reload();
    }

    /**
     * 기동 완료 후 재빌드. {@code @PostConstruct} 만으로는 부족하다 — 참조데이터 시드
     * ({@code ReferenceDataSeeder})가 Flyway 순환 의존을 피하려고 {@code ApplicationRunner} 로
     * 실행되는데, 그건 컨텍스트 refresh 가 끝난 <b>뒤</b>다. 즉 빈 DB 로 첫 기동하면
     * {@code @PostConstruct} 는 0행 상태를 읽고, 시드가 143행을 넣어도 재빌드 트리거가 없어
     * 카테고리 조회가 빈 배열을 반환한다(2회차 기동부터는 행이 있어 정상).
     *
     * <p>{@code @PostConstruct} 를 이 리스너로 <b>교체</b>하지 않고 더한 이유는 위의 null 창
     * 때문이다. 기동 시 SELECT 가 한 번 더 나가지만 순서에 무관하게 항상 옳다.
     * {@code ApplicationReadyEvent} 는 모든 {@code ApplicationRunner} 이후에 발행된다.
     */
    @EventListener(ApplicationReadyEvent.class)
    void reloadAfterStartup() {
        reload();
    }

    public void reload (){
        this.categorySnapshot = build(categoryRepository.findAllByOrderByDisplayOrderAsc());
    }

    // 2 패스조립
    private CategorySnapshot build(List<Category> all){
        Map<Long, CategoryNode> byId = new HashMap<>();
        for (Category c : all){
            byId.put(c.getId(), new CategoryNode(c));
        }

        List<CategoryNode> roots = new ArrayList<>();
        Map<String, CategoryNode> byCode = new HashMap<>();
        for(Category c : all){
            CategoryNode node = byId.get(c.getId());
            byCode.put(node.getSlug(), node);
            if(c.getParentId() == null) {
                roots.add(node);
                continue;
            }
            CategoryNode parent = byId.get(c.getParentId());
            if(parent == null) {
                // 부모가 조회 결과에 없는 고아 노드 -> 트리에서 제외(리로드 전체 실패 방지)
                continue;
            }
            parent.addChild(node);
        }

        return new CategorySnapshot(roots, byCode, byId);
    }

    // 읽기 API(DB 안 탐)
    public List<CategoryNode> getRoots(){
        return categorySnapshot.getRoots();
    }

    public CategoryNode getByCode (String code){
        CategoryNode node = categorySnapshot.findByCode(code);
        if(node == null) throw new BusinessException(CategoryErrorException.CATEGORY_NOT_FOUND);

        return node;
    }

    // 대분류, 중분류에 속한 company들 다 가져옴
    //노드의 서브트리 id 전부 - IN 쿼리용
    public List<Long> collectSubtreeIds (String code){
        CategoryNode root = getByCode(code);
        List<Long> ids = new ArrayList<>();
        Deque<CategoryNode> stack = new ArrayDeque<>();

        stack.push(root);
        while (!stack.isEmpty()){
            CategoryNode n = stack.pop();
            ids.add(n.getId());
            n.getChildren().forEach(stack::push);
        }

        return ids;
    }



}
