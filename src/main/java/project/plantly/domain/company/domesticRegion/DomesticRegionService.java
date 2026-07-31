package project.plantly.domain.company.domesticRegion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.domesticRegion.dto.DomesticRegionAdminResponse;
import project.plantly.domain.company.domesticRegion.dto.DomesticRegionPublicResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DomesticRegionService {

    private final DomesticRegionRepository domesticRegionRepository;

    // 관리자용 행정구역 트리 조회 — 시도(루트) 아래 시군구(자식)를 중첩. 자식이 없는 시도는 children 이 빈 리스트.
    @Transactional(readOnly = true)
    public List<DomesticRegionAdminResponse> getTree() {
        List<DomesticRegion> all = domesticRegionRepository.findAllByOrderByCodeAsc();

        // 부모 code -> 자식 목록 (code 순 유지). 시군구만 parentCode 를 가진다.
        Map<String, List<DomesticRegion>> childrenByParent = all.stream()
                .filter(region -> region.getParentCode() != null)
                .collect(Collectors.groupingBy(DomesticRegion::getParentCode));

        // 루트(시도) = parentCode 없음
        return all.stream()
                .filter(region -> region.getParentCode() == null)
                .map(root -> toResponse(root, childrenByParent))
                .toList();
    }

    /**
     * 공개 지역 옵션 트리 — 등록/수정 폼의 국내 커버리지 선택 소스.
     *
     * <p>관리자 트리와 달리 {@code active=false} 는 제외한다. 비활성 시도의 하위 시군구는
     * 활성이더라도 붙을 루트가 없어 함께 사라지는데, 이게 의도한 동작이다 — 시도를 내렸으면
     * 그 안의 시군구도 더는 고를 수 없어야 한다.
     *
     * <p>루트에는 전국(NATION)과 시도(SIDO)가 섞여 나오고, 둘 다 {@code parentCode} 가 없다.
     * 전국을 3단 트리의 최상위로 두지 않은 건 의도적이다 — 그렇게 하면 "children 이 비면 확정"
     * 이라는 프론트 규칙과 기존 트리 조립 코드가 모두 특수 케이스를 떠안는다.
     */
    @Transactional(readOnly = true)
    public List<DomesticRegionPublicResponse> getPublicTree() {
        List<DomesticRegion> actives = domesticRegionRepository.findByActiveTrueOrderByCodeAsc();

        Map<String, List<DomesticRegion>> childrenByParent = actives.stream()
                .filter(region -> region.getParentCode() != null)
                .collect(Collectors.groupingBy(DomesticRegion::getParentCode));

        return actives.stream()
                .filter(region -> region.getParentCode() == null)
                .map(root -> toPublicResponse(root, childrenByParent))
                .toList();
    }

    private DomesticRegionPublicResponse toPublicResponse(DomesticRegion region, Map<String, List<DomesticRegion>> childrenByParent) {
        List<DomesticRegionPublicResponse> children = childrenByParent
                .getOrDefault(region.getCode(), List.of())
                .stream()
                .map(child -> toPublicResponse(child, childrenByParent))
                .toList();

        return DomesticRegionPublicResponse.of(region, children);
    }

    // 재귀 조립 — 잎 노드(시군구)는 children 이 빈 리스트가 된다.
    private DomesticRegionAdminResponse toResponse(DomesticRegion region, Map<String, List<DomesticRegion>> childrenByParent) {
        List<DomesticRegionAdminResponse> children = childrenByParent
                .getOrDefault(region.getCode(), List.of())
                .stream()
                .map(child -> toResponse(child, childrenByParent))
                .toList();

        return DomesticRegionAdminResponse.of(region, children);
    }
}
