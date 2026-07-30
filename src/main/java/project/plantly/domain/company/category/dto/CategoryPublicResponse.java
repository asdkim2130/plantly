package project.plantly.domain.company.category.dto;

import project.plantly.domain.company.category.tree.CategoryNode;

import java.util.List;

/**
 * 공개 카테고리 옵션. 회사 등록 폼의 대→중→소 3단 선택과 검색 필터 패널이 쓴다.
 *
 * <p>인증/산업과 달리 평면이 아니라 중첩 트리로 내려준다 — 3단 선택 자체가 부모-자식 관계를 UI 로
 * 드러내는 구조라 프론트가 parentId 로 다시 조립할 이유가 없다. {@code depth} 는 중첩 위치로도
 * 알 수 있지만, 대/중/소분류 라벨이 이 값에 1:1 대응하고 엔티티의 실제 속성이라 함께 내려준다
 * ({@code CompanyPublicResponse.CategoryResponse} 와 필드 구성을 맞춤).
 *
 * <p>운영 필드(displayOrder/active)는 노출하지 않는다 — 정렬은 서버가 이미 적용했고,
 * 비활성 항목은 애초에 목록에서 빠진다. description 도 뺐다: 시드가 값을 넣지 않아 전량 null 이다.
 * ({@code CertificationPublicResponse}·{@code IndustryPublicResponse} 와 동일한 기준)
 */
public record CategoryPublicResponse(
        Long id,
        String categoryName,
        String slug,
        int depth,
        String iconUrl,
        List<CategoryPublicResponse> children
) {

    /**
     * 활성 노드만 남긴 중첩 트리로 변환한다.
     *
     * <p>비활성 노드는 <b>서브트리째</b> 빠진다. 자식이 활성이어도 부모가 폐기됐다면 3단 선택에서
     * 도달할 경로가 없고, 대분류 폐기는 "그 분류 전체를 신규 선택에서 뺀다"는 뜻이기 때문이다.
     * 루트와 자식에 같은 규칙이 적용되도록 재귀 한 곳에서만 필터링한다.
     *
     * <p>검색은 이 필터를 타지 않는다 — 패싯은 {@code company_category_closure} 를 SQL 로 직접
     * 매칭하므로(스냅샷 경유 아님), 이미 폐기된 카테고리를 링크한 회사도 조상 closure 로 계속 잡힌다.
     * 다만 그 id 는 이 목록에서 빠지므로 클라이언트가 알 방법이 없다 — 폐기 = 검색에서도 즉시 사라짐.
     */
    public static List<CategoryPublicResponse> activeTreeOf(List<CategoryNode> nodes) {
        return nodes.stream()
                .filter(CategoryNode::isActive)
                .map(node -> new CategoryPublicResponse(
                        node.getId(),
                        node.getCategoryName(),
                        node.getSlug(),
                        node.getDepth(),
                        node.getIconUrl(),
                        activeTreeOf(node.getChildren())))
                .toList();
    }
}
