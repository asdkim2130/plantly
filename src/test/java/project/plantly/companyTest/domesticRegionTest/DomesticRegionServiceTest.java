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
        DomesticRegion gyeonggi = DomesticRegion.create("4100000000", "경기도", RegionLevel.SIDO, null);
        DomesticRegion suwon = DomesticRegion.create("4111000000", "경기도 수원시", RegionLevel.SIGUNGU, "4100000000");
        DomesticRegion sejong = DomesticRegion.create("3611000000", "세종특별자치시", RegionLevel.SIDO, null);

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
        DomesticRegion busan = DomesticRegion.create("2600000000", "부산광역시", RegionLevel.SIDO, null);
        busan.deactivate();
        given(domesticRegionRepository.findAllByOrderByCodeAsc()).willReturn(List.of(busan));

        List<DomesticRegionAdminResponse> tree = domesticRegionService.getTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).active()).isFalse();
    }

    @Test
    @DisplayName("행정구역이 없으면 빈 리스트를 반환")
    public void getTree_empty (){
        given(domesticRegionRepository.findAllByOrderByCodeAsc()).willReturn(List.of());

        assertThat(domesticRegionService.getTree()).isEmpty();
    }
}
