package project.plantly.domain.company.category.tree;

import java.util.List;
import java.util.Map;


public class CategorySnapshot {

    private final List<CategoryNode> roots;  // 대분류 forest
    private final Map<String, CategoryNode> byCode;  //슬러그 -> 노드
    private final Map<Long, CategoryNode> byId;  // id -> 노드

    public CategorySnapshot(List<CategoryNode> roots, Map<String, CategoryNode> byCode, Map<Long, CategoryNode> byId){
        this.roots = List.copyOf(roots);  // 불변화
        this.byCode = Map.copyOf(byCode);  // 불변화
        this.byId = Map.copyOf(byId);  // 불변화
    }

    // lombok 경고 무시
    public List<CategoryNode> getRoots (){
        return roots;
    }

    public CategoryNode findByCode (String code){
        return byCode.get(code);
    }

    public CategoryNode findById (Long id){
        return byId.get(id);
    }

}
