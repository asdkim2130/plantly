package project.plantly.domain.company.domesticRegion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.domesticRegion.dto.DomesticRegionAdminResponse;
import project.plantly.domain.company.domesticRegion.dto.DomesticRegionPublicResponse;

import java.util.Comparator;
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

        Map<String, List<DomesticRegion>> childrenByParent = groupChildrenByParent(all);

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

        Map<String, List<DomesticRegion>> childrenByParent = groupChildrenByParent(actives);

        return actives.stream()
                .filter(region -> region.getParentCode() == null)
                .map(root -> toPublicResponse(root, childrenByParent))
                .toList();
    }

    /**
     * 부모 code -> 자식 목록. 시군구만 {@code parentCode} 를 가지므로 잎 노드만 모인다.
     *
     * <p>자식은 {@code shortName}("수원" / "오산") 가나다순으로 정렬한다. 루트(전국·시도)는
     * 리포지토리가 준 code 순 그대로 둔다 — 법정동코드 순서가 곧 서울·부산·… 이라는
     * 관습적 배열이라, 여기서만 순서를 바꾸면 오히려 낯설어진다.
     *
     * <p>정렬을 {@code ORDER BY} 가 아니라 애플리케이션에서 하는 이유는 {@code CountryService}
     * 와 같다 — 한글 정렬은 DB collation 에 좌우되고(컨테이너 기본 en_US.utf8 에서 실제로
     * 순서가 어긋난다), 운영 DB 로케일이 다르면 또 바뀐다. {@code String} 자연 순서면 충분하다:
     * 한글 음절(U+AC00~U+D7A3)이 초성-중성-종성 순으로 배열돼 코드포인트 순서가 곧 가나다순이고,
     * 시드의 {@code shortName} 은 전부 한글 음절이다.
     *
     * <p>{@code shortName} 은 DB 제약이 없어 이론상 null 이 가능하다(엔티티 주석 참고).
     * 시드가 항상 채우지만, 빠진 행 하나 때문에 옵션 API 전체가 죽지 않도록 null 은 뒤로 보낸다.
     */
    private Map<String, List<DomesticRegion>> groupChildrenByParent(List<DomesticRegion> regions) {
        Map<String, List<DomesticRegion>> childrenByParent = regions.stream()
                .filter(region -> region.getParentCode() != null)
                .collect(Collectors.groupingBy(DomesticRegion::getParentCode));

        Comparator<DomesticRegion> byShortName = Comparator.comparing(
                DomesticRegion::getShortName, Comparator.nullsLast(Comparator.naturalOrder()));
        childrenByParent.values().forEach(children -> children.sort(byShortName));

        return childrenByParent;
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
