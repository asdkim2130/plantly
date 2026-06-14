package project.plantly.domain.company.category.tree;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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

    // 기동 시 1회 빌드
    @PostConstruct
    void init() {
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
            byCode.put(node.getCategoryCode(), node);
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
