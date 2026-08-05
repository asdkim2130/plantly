package project.plantly.global.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.category.Category;
import project.plantly.domain.company.category.CategoryRepository;
import project.plantly.domain.company.certification.Certification;
import project.plantly.domain.company.certification.CertificationRepository;
import project.plantly.domain.company.country.Country;
import project.plantly.domain.company.country.CountryRepository;
import project.plantly.domain.company.domesticRegion.DomesticRegion;
import project.plantly.domain.company.domesticRegion.DomesticRegionRepository;
import project.plantly.domain.company.domesticRegion.RegionLevel;
import project.plantly.domain.company.industry.Industry;
import project.plantly.domain.company.industry.IndustryRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 마스터 데이터 카탈로그.
 *
 * <p><b>마스터를 만들지 않고 읽기만 한다</b>는 것이 이 클래스의 전부다. 시드가 카테고리·지역을 직접
 * 만들면 {@code slug}/{@code code} UNIQUE 에 걸리고(기존 테스트 시더가 겪는 문제), 무엇보다 마스터가
 * 바뀔 때마다 시드도 같이 고쳐야 하는 두 번째 진실원이 생긴다. 여기서 뽑아 쓰면 FK 정합은 공짜다.
 *
 * <p>샘플링은 난수 없이 호출부 순번의 나머지 연산으로만 한다 — 같은 순서로 심으면 같은 결과가 나온다.
 */
@Slf4j
@Component
@Profile(SeedConfig.PROFILE)
@RequiredArgsConstructor
public class SeedMasterCatalog {

    private final CategoryRepository categoryRepository;
    private final IndustryRepository industryRepository;
    private final CertificationRepository certificationRepository;
    private final CountryRepository countryRepository;
    private final DomesticRegionRepository domesticRegionRepository;

    private Map<Long, Category> categoriesById;
    private List<Category> leafCategories;
    private List<Category> rootCategories;
    private List<Industry> industries;
    private List<Certification> certifications;
    private List<Country> countries;
    private List<DomesticRegion> regions;

    /** 마스터를 한 번 읽어 보관한다. 하나라도 비어 있으면 시드를 진행하지 않는다. */
    public void load() {
        List<Category> categories = categoryRepository.findAllByOrderByDisplayOrderAsc();
        categoriesById = categories.stream().collect(Collectors.toMap(Category::getId, Function.identity()));
        // 소분류(depth=3)만 회사에 붙인다. 대분류 선택 시 closure 로 잡히는지가 패싯 검증의 핵심이라,
        // 회사를 대분류에 직접 매달면 그 검증이 무의미해진다.
        leafCategories = categories.stream().filter(c -> c.getDepth() == 3).toList();
        rootCategories = categories.stream().filter(c -> c.getDepth() == 1).toList();

        industries = industryRepository.findAllByOrderByDisplayOrderAsc();
        certifications = certificationRepository.findAllByOrderByDisplayOrderAsc();
        countries = countryRepository.findAll();
        // 광역시·특별시는 커버리지 모델상 시도 행 자체가 지역이라 SIGUNGU 자식이 없다. 시군구만 쓰면
        // 서울·부산이 통째로 빠져 지역 필터가 도(道) 지역만 검증하게 되므로 둘 다 풀에 넣는다.
        // NATION(전국)은 검색 필터용 합성 행이지 회사의 소재지가 아니므로 제외한다.
        regions = domesticRegionRepository.findAllByOrderByCodeAsc().stream()
                .filter(r -> r.getLevel() != RegionLevel.NATION)
                .toList();

        require(leafCategories, "카테고리(소분류)");
        require(rootCategories, "카테고리(대분류)");
        require(industries, "산업");
        require(certifications, "인증");
        require(countries, "국가");
        require(regions, "국내 지역");

        log.info("[seed] 마스터 로드 — 소분류 {}, 대분류 {}, 산업 {}, 인증 {}, 국가 {}, 지역 {}",
                leafCategories.size(), rootCategories.size(), industries.size(),
                certifications.size(), countries.size(), regions.size());
    }

    private void require(List<?> values, String label) {
        if (values.isEmpty()) {
            throw new IllegalStateException(
                    "마스터 데이터(" + label + ")가 비어 있어 시드를 진행할 수 없습니다. "
                            + "ReferenceDataSeeder 가 정상 실행됐는지(= local 프로파일, Postgres 연결) 확인하세요.");
        }
    }

    // ===== 결정론적 샘플링 =====
    // seed 는 "몇 번째 회사인가" 같은 호출부의 순번이다. 같은 순번이면 항상 같은 마스터가 나온다.

    public List<Long> categoryIds(int seed, int count) {
        return pickIds(leafCategories, seed, count, Category::getId);
    }

    public List<Long> industryIds(int seed, int count) {
        return pickIds(industries, seed, count, Industry::getId);
    }

    public List<Long> certificationIds(int seed, int count) {
        return pickIds(certifications, seed, count, Certification::getId);
    }

    public List<Long> countryIds(int seed, int count) {
        return pickIds(countries, seed, count, Country::getId);
    }

    public List<Long> regionIds(int seed, int count) {
        return pickIds(regions, seed, count, DomesticRegion::getId);
    }

    /** 회사 주소로 쓸 지역. 지역 필터와 주소 문자열이 어긋나지 않도록 같은 행에서 둘 다 만든다. */
    public DomesticRegion region(int seed) {
        return regions.get(Math.floorMod(seed, regions.size()));
    }

    // 시군구 단계가 없는 게 실제인 시도. 주소가 시도 하나로 끝나는 유일한 정상 케이스라 손대지 않는다.
    private static final String NO_SIGUNGU_SIDO = "세종특별자치시";

    /**
     * 주소 문자열 앞에 붙일 지역 명칭을 "시도 시군구" 형태로 돌려준다.
     *
     * <p>지역 풀에는 시군구뿐 아니라 시도 행도 들어 있는데({@link #load()} 참고), 시도 명칭을 그대로 쓰면
     * 주소가 "서울특별시 산업로 100" 이 되어 카드 지역 라벨(시도+시군구)이 도로명 조각을 물게 된다.
     * 실제 주소도 세종을 빼면 시도 하나로 끝나지 않으므로, 시군구 토큰을 채워 형태를 실제와 맞춘다.
     *
     * <p>도(道)는 마스터에 실제 자식 시군구가 있으니 그중 하나를 쓰고("경기도" → "경기도 수원시"),
     * 자식이 없는 광역시·특별시만 구 이름을 지어 붙인다("서울특별시" → "서울특별시 중구").
     * 회사가 연결된 지역 행은 그대로이므로 지역 필터와 주소는 여전히 같은 시도를 가리킨다.
     */
    public String addressRegionName(DomesticRegion region, int seed) {
        if (region.getLevel() == RegionLevel.SIGUNGU || NO_SIGUNGU_SIDO.equals(region.getName())) {
            return region.getName();
        }
        List<DomesticRegion> children = regions.stream()
                .filter(r -> region.getCode().equals(r.getParentCode()))
                .toList();
        if (children.isEmpty()) {
            return region.getName() + " " + SeedVocabulary.district(seed);
        }
        // 자식 명칭은 이미 "경기도 수원시" 형태(행안부 원본)라 그대로 쓰면 된다.
        return children.get(Math.floorMod(seed, children.size())).getName();
    }

    public Category leafCategory(int seed) {
        return leafCategories.get(Math.floorMod(seed, leafCategories.size()));
    }

    public Category categoryById(Long id) {
        return categoriesById.get(id);
    }

    /** 소분류의 대분류 조상. C23(소분류 전용 회사)이 어느 대분류로 잡혀야 하는지 매니페스트에 적기 위한 것. */
    public Category rootAncestorOf(Category leaf) {
        Category current = leaf;
        while (current.getParentId() != null) {
            Category parent = categoriesById.get(current.getParentId());
            if (parent == null) {
                return current;
            }
            current = parent;
        }
        return current;
    }

    /**
     * 목록에서 count 개를 겹치지 않게 고른다. 시작점을 seed 로 옮기고 일정 간격으로 건너뛰어, 순번이 인접한
     * 회사들이 똑같은 마스터 조합을 갖지 않게 한다 — 패싯 필터가 늘 같은 결과를 주면 검증이 되지 않는다.
     * 간격이 겹쳐 개수가 모자라면 뒤이어 순차로 채워 항상 요청한 만큼(또는 마스터 전체) 돌려준다.
     */
    private <T> List<Long> pickIds(List<T> source, int seed, int count, Function<T, Long> idOf) {
        int size = source.size();
        int take = Math.min(count, size);
        if (take <= 0) {
            return List.of();
        }
        int stride = Math.max(1, size / take);
        int start = Math.floorMod(seed * 7, size);

        LinkedHashSet<Long> picked = new LinkedHashSet<>();
        for (int i = 0; i < take; i++) {
            picked.add(idOf.apply(source.get(Math.floorMod(start + i * stride, size))));
        }
        // 간격 충돌로 모자라면 시작점부터 순차로 메운다.
        for (int i = 0; picked.size() < take && i < size; i++) {
            picked.add(idOf.apply(source.get(Math.floorMod(start + i, size))));
        }
        return new ArrayList<>(picked);
    }
}
