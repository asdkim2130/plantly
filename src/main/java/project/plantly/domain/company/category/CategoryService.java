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

    /**
     * 공개 트리에 실제로 실리는 노드 수(현황 지표의 '솔루션 분류').
     *
     * <p>{@code active = true} 인 행을 세는 것으로는 답이 나오지 않는다. 트리 매핑은 비활성 노드에서
     * 재귀를 멈추므로, <b>비활성 부모 아래의 활성 자식은 통째로 빠진다</b> — 평면 count 는 그 자식을 세지만
     * 화면에는 없다. 그래서 조건을 다시 적지 않고 위 메서드가 만든 결과를 그대로 센다. 규칙이 한 곳에만
     * 있으므로 노출 기준이 바뀌어도 두 값이 갈라지지 않는다.
     *
     * <p>스냅샷이 메모리라 트리를 다시 만드는 비용은 DB 왕복이 아니라 객체 매핑뿐이다.
     */
    public long countPublicTree (){

        return count(getPublicTree());
    }

    private long count (List<CategoryPublicResponse> nodes){

        return nodes.stream().mapToLong(node -> 1 + count(node.children())).sum();
    }
}
