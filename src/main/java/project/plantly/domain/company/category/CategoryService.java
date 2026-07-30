package project.plantly.domain.company.category;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.plantly.domain.company.category.dto.CategoryPublicResponse;
import project.plantly.domain.company.category.tree.CategoryTreeService;

import java.util.List;

/**
 * 카테고리 공개 조회. 쓰기와 관리자 조회는 {@link CategoryAdminService} 가 담당한다.
 *
 * <p>DTO 매핑을 여기서 하는 이유는 {@link CategoryTreeService} 를 dto 의존 없는 트리/캐시
 * 컴포넌트로 유지하기 위해서다 — 관리자 조회도 같은 스냅샷을 각자 DTO 로 매핑한다.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryTreeService treeService;

    /**
     * 공개 카테고리 트리 — 등록 폼/검색 필터의 대→중→소 선택지. 활성 노드만, displayOrder 순.
     *
     * <p>DB 를 타지 않는다: 기동 시 빌드된 메모리 스냅샷을 읽으므로 {@code @Transactional} 이 없다
     * (관리자 조회도 동일). 스냅샷에는 비활성 노드도 있어 매핑 단계에서 걸러낸다.
     */
    public List<CategoryPublicResponse> getPublicTree (){

        return CategoryPublicResponse.activeTreeOf(treeService.getRoots());
    }
}
