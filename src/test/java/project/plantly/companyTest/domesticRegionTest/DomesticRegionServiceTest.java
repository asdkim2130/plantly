package project.plantly.companyTest.domesticRegionTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.plantly.domain.company.domesticRegion.DomesticRegion;
import project.plantly.domain.company.domesticRegion.DomesticRegionRepository;
import project.plantly.domain.company.domesticRegion.DomesticRegionService;
import project.plantly.domain.company.domesticRegion.RegionLevel;
import project.plantly.domain.company.domesticRegion.dto.DomesticRegionAdminResponse;
import project.plantly.domain.company.domesticRegion.dto.DomesticRegionPublicResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
public class DomesticRegionServiceTest {

    @Mock DomesticRegionRepository domesticRegionRepository;
    @InjectMocks DomesticRegionService domesticRegionService;

    @Test
    @DisplayName("시도(루트) 아래 시군구(자식)를 트리로 조립하고, 자식 없는 시도는 children이 빈 리스트")
    public void getTree_buildsTree (){
        DomesticRegion gyeonggi = gyeonggi();
        DomesticRegion suwon = suwon();
        DomesticRegion sejong = sejong();

        // code 오름차순으로 반환된다고 가정 (3611... < 4100... < 4111...)
        given(domesticRegionRepository.findAllByOrderByCodeAsc())
                .willReturn(List.of(sejong, gyeonggi, suwon));

        List<DomesticRegionAdminResponse> tree = domesticRegionService.getTree();

        // 루트는 parentCode 없는 시도 2개 (세종, 경기도)
        assertThat(tree).hasSize(2);

        DomesticRegionAdminResponse sejongNode = tree.get(0);
        assertThat(sejongNode.name()).isEqualTo("세종특별자치시");
        assertThat(sejongNode.active()).isTrue();
        assertThat(sejongNode.children()).isEmpty();   // 자식 없는 시도는 단독

        DomesticRegionAdminResponse gyeonggiNode = tree.get(1);
        assertThat(gyeonggiNode.name()).isEqualTo("경기도");
        assertThat(gyeonggiNode.children()).hasSize(1);
        assertThat(gyeonggiNode.children().get(0).name()).isEqualTo("경기도 수원시");
        assertThat(gyeonggiNode.children().get(0).children()).isEmpty();
    }

    @Test
    @DisplayName("active=false 인 행정구역도 그대로 매핑된다")
    public void getTree_mapsActiveFlag (){
        DomesticRegion busan = DomesticRegion.create("2600000000", "부산광역시", "부산", "부산", RegionLevel.SIDO, null);
        busan.deactivate();
        given(domesticRegionRepository.findAllByOrderByCodeAsc()).willReturn(List.of(busan));

        List<DomesticRegionAdminResponse> tree = domesticRegionService.getTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).active()).isFalse();
    }

    @Test
    @DisplayName("관리자 트리의 자식(시군구)은 shortName 가나다순으로 정렬된다")
    public void getTree_sortsChildrenByShortName (){
        // 리포지토리는 code 순으로 준다: 수원(4111) < 오산(4137) < 광주(4161).
        // 가나다순이면 광주 → 수원 → 오산 이어야 한다.
        given(domesticRegionRepository.findAllByOrderByCodeAsc())
                .willReturn(List.of(gyeonggi(), suwon(), osan(), gwangju()));

        List<DomesticRegionAdminResponse> tree = domesticRegionService.getTree();

        assertThat(tree.get(0).children())
                .extracting(DomesticRegionAdminResponse::name)
                .containsExactly("경기도 광주시", "경기도 수원시", "경기도 오산시");
    }

    @Test
    @DisplayName("행정구역이 없으면 빈 리스트를 반환")
    public void getTree_empty (){
        given(domesticRegionRepository.findAllByOrderByCodeAsc()).willReturn(List.of());

        assertThat(domesticRegionService.getTree()).isEmpty();
    }

    // ===== 공개 옵션 트리 =====

    @Test
    @DisplayName("공개 트리는 전국·시도를 루트로 두고 시도 아래에만 시군구를 중첩한다")
    public void getPublicTree_buildsTree (){
        // code 오름차순 (0000... < 3611... < 4100... < 4111...)
        given(domesticRegionRepository.findByActiveTrueOrderByCodeAsc())
                .willReturn(List.of(nationwide(), sejong(), gyeonggi(), suwon()));

        List<DomesticRegionPublicResponse> tree = domesticRegionService.getPublicTree();

        // 전국은 시도의 부모가 아니라 형제 루트다 — 3단 트리가 되면 프론트의 "children 이 비면 확정"
        // 규칙이 깨지므로, 루트에 전국과 시도가 나란히 온다.
        assertThat(tree).hasSize(3);

        // 합성 코드 0000000000 덕분에 전국이 항상 첫 항목이 된다 (1차 드롭다운 맨 위).
        DomesticRegionPublicResponse nation = tree.get(0);
        assertThat(nation.level()).isEqualTo(RegionLevel.NATION);
        assertThat(nation.shortName()).isEqualTo("전국");
        assertThat(nation.displayName()).isEqualTo("전국");
        assertThat(nation.children()).isEmpty();

        // 자식 없는 시도는 1차에서 바로 확정 — short/display 가 같다.
        DomesticRegionPublicResponse sejongNode = tree.get(1);
        assertThat(sejongNode.shortName()).isEqualTo("세종");
        assertThat(sejongNode.displayName()).isEqualTo("세종");
        assertThat(sejongNode.children()).isEmpty();

        // 자식 있는 시도는 2차가 열리고, 시도 자신의 displayName 이 '전역' 배지가 된다.
        DomesticRegionPublicResponse gyeonggiNode = tree.get(2);
        assertThat(gyeonggiNode.shortName()).isEqualTo("경기");
        assertThat(gyeonggiNode.displayName()).isEqualTo("경기 전역");
        assertThat(gyeonggiNode.children()).hasSize(1);

        // 시군구는 드롭다운용으로 부모명 없는 이름, 배지용으로 부모명 붙은 이름을 함께 받는다.
        DomesticRegionPublicResponse suwonNode = gyeonggiNode.children().get(0);
        assertThat(suwonNode.shortName()).isEqualTo("수원");
        assertThat(suwonNode.displayName()).isEqualTo("경기 수원");
        assertThat(suwonNode.children()).isEmpty();
    }

    @Test
    @DisplayName("공개 트리는 비활성 지역을 제외하고, 비활성 시도의 하위 시군구도 함께 사라진다")
    public void getPublicTree_excludesInactive (){
        // 리포지토리가 활성만 돌려주므로 비활성 경기도는 애초에 목록에 없다.
        // 활성인 수원시만 남지만 붙을 루트가 없어 응답에서 빠진다 — 시도를 내리면 그 안도 못 고른다.
        given(domesticRegionRepository.findByActiveTrueOrderByCodeAsc())
                .willReturn(List.of(sejong(), suwon()));

        List<DomesticRegionPublicResponse> tree = domesticRegionService.getPublicTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).shortName()).isEqualTo("세종");
    }

    @Test
    @DisplayName("공개 트리도 자식(시군구)만 가나다순으로 정렬하고 루트는 code 순을 지킨다")
    public void getPublicTree_sortsChildrenByShortName (){
        // code 순 입력: 전국(0000) < 세종(3611) < 경기(4100) < 수원(4111) < 오산(4137) < 광주(4161)
        given(domesticRegionRepository.findByActiveTrueOrderByCodeAsc())
                .willReturn(List.of(nationwide(), sejong(), gyeonggi(), suwon(), osan(), gwangju()));

        List<DomesticRegionPublicResponse> tree = domesticRegionService.getPublicTree();

        // 루트는 code 순 그대로 — 전국이 맨 앞, 그다음 세종·경기.
        assertThat(tree)
                .extracting(DomesticRegionPublicResponse::shortName)
                .containsExactly("전국", "세종", "경기");

        // 자식만 가나다순으로 재정렬된다.
        assertThat(tree.get(2).children())
                .extracting(DomesticRegionPublicResponse::shortName)
                .containsExactly("광주", "수원", "오산");
    }

    @Test
    @DisplayName("활성 행정구역이 없으면 공개 트리는 빈 리스트를 반환")
    public void getPublicTree_empty (){
        given(domesticRegionRepository.findByActiveTrueOrderByCodeAsc()).willReturn(List.of());

        assertThat(domesticRegionService.getPublicTree()).isEmpty();
    }

    // 시드(domestic-region.sql)의 실제 값과 같은 표기를 쓴다.
    private DomesticRegion nationwide() {
        return DomesticRegion.create("0000000000", "전국", "전국", "전국", RegionLevel.NATION, null);
    }

    private DomesticRegion sejong() {
        return DomesticRegion.create("3611000000", "세종특별자치시", "세종", "세종", RegionLevel.SIDO, null);
    }

    private DomesticRegion gyeonggi() {
        return DomesticRegion.create("4100000000", "경기도", "경기", "경기 전역", RegionLevel.SIDO, null);
    }

    private DomesticRegion suwon() {
        return DomesticRegion.create("4111000000", "경기도 수원시", "수원", "경기 수원", RegionLevel.SIGUNGU, "4100000000");
    }

    private DomesticRegion osan() {
        return DomesticRegion.create("4137000000", "경기도 오산시", "오산", "경기 오산", RegionLevel.SIGUNGU, "4100000000");
    }

    private DomesticRegion gwangju() {
        return DomesticRegion.create("4161000000", "경기도 광주시", "광주", "경기 광주", RegionLevel.SIGUNGU, "4100000000");
    }
}
