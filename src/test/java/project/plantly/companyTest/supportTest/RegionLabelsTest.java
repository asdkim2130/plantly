package project.plantly.companyTest.supportTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import project.plantly.domain.company.support.RegionLabels;

import static org.assertj.core.api.Assertions.assertThat;

// RegionLabels: 카드 표기용 지역 라벨(시도+시군구) 파생. 카카오 우편번호 서비스가 내려주는 도로명 주소 형태가 입력이다.
@DisplayName("RegionLabels: 도로명 주소 → 지역 라벨")
class RegionLabelsTest {

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("시도+시군구까지만 남긴다")
    @CsvSource({
            "'서울시 강남구 테헤란로 1', '서울시 강남구'",
            "'경기 화성시 동탄대로 45', '경기 화성시'",
            "'부산시 해운대구 센텀중앙로 90', '부산시 해운대구'",
            "'제주특별자치도 제주시 첨단로 242', '제주특별자치도 제주시'"
    })
    void keepsSidoAndSigungu(String roadAddress, String expected) {
        assertThat(RegionLabels.fromRoadAddress(roadAddress)).isEqualTo(expected);
    }

    @Test
    @DisplayName("일반시의 구는 3번째 토큰이라 잘려나가고 시 단위까지만 남는다")
    void generalCityDistrict_trimmedToCity() {
        assertThat(RegionLabels.fromRoadAddress("경기 성남시 분당구 판교역로 235")).isEqualTo("경기 성남시");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("시군구 단계가 없는 세종은 1토큰만 남긴다 — 2토큰을 자르면 도로명이 딸려온다")
    @ValueSource(strings = {"세종특별자치시 한누리대로 2130", "세종시 한누리대로 2130", "세종 한누리대로 2130"})
    void sejong_hasNoSigungu(String roadAddress) {
        assertThat(RegionLabels.fromRoadAddress(roadAddress)).doesNotContain("한누리대로");
    }

    @Test
    @DisplayName("토큰이 하나뿐이면 자를 게 없으므로 그대로 돌려준다")
    void singleToken_returnedAsIs() {
        assertThat(RegionLabels.fromRoadAddress("서울시")).isEqualTo("서울시");
    }

    @Test
    @DisplayName("앞뒤 공백과 토큰 사이 연속 공백을 흡수한다")
    void normalizesWhitespace() {
        assertThat(RegionLabels.fromRoadAddress("  경기   화성시  동탄대로 45 ")).isEqualTo("경기 화성시");
    }

    @Test
    @DisplayName("null·공백은 그대로 돌려준다 (카드 프로젝션이 그대로 흘려보낸다)")
    void nullOrBlank_returnedAsIs() {
        assertThat(RegionLabels.fromRoadAddress(null)).isNull();
        assertThat(RegionLabels.fromRoadAddress("   ")).isEqualTo("   ");
    }
}
