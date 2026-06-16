package project.plantly.domain.company.domesticRegion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.domesticRegion.dto.DomesticRegionAdminResponse;

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
