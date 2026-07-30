package project.plantly.companyTest.countryTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import project.plantly.companyTest.support.PostgresContainerTest;
import project.plantly.domain.company.country.Continent;
import project.plantly.domain.company.country.Country;
import project.plantly.domain.company.country.CountryRepository;
import project.plantly.domain.company.country.CountryService;
import project.plantly.domain.company.country.dto.CountryPublicResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 국가 목록 정렬을 운영 DB(Postgres)에서 고정한다.
 *
 * <p>{@code Country} 에는 displayOrder 가 없어 정렬 기준이 국가명(한글)뿐인데, 문자열 정렬을
 * {@code ORDER BY} 에 맡기면 DB collation 에 좌우된다. 실제로 Postgres 15 컨테이너
 * (기본 로케일 en_US.utf8)의 {@code ORDER BY name_ko} 는
 * {@code 가나 · 남극 · 독일 · 베트남 · 대한민국} 을 내놓는다 — "대한민국"이 "독일"보다 뒤다.
 * 그래서 정렬을 {@code CountryService} 로 옮겼고, 이 테스트가 그 결정을 지킨다.
 *
 * <p>test 프로파일의 H2 는 Java String 비교라 이 차이가 드러나지 않는다. 누가 정렬을 다시
 * {@code ORDER BY} 로 돌려놓으면 H2 슬라이스·인수 테스트는 통과하고 여기서만 깨진다.
 */
class CountryOrderingPostgresTest extends PostgresContainerTest {

    @Autowired
    CountryRepository countryRepository;

    @Autowired
    CountryService countryService;

    @Test
    @DisplayName("Postgres 에서도 공개 목록은 가나다순으로 나온다 (DB collation 에 좌우되지 않는다)")
    void getPublicList_sortsInKoreanAlphabeticalOrderOnPostgres() {
        countryRepository.deleteAll();   // 컨테이너를 클래스 간 공유하므로 잔여 행을 지운다

        // 저장 순서를 가나다순과 어긋나게 섞는다. 초성이 겹치는 대한민국/독일이 핵심 케이스로,
        // Postgres 의 collation 순서와 가나다순이 갈리는 지점이다.
        countryRepository.saveAll(List.of(
                Country.create("VN", "VNM", "704", "베트남", "Viet Nam", Continent.ASIA),
                Country.create("DE", "DEU", "276", "독일", "Germany", Continent.EUROPE),
                Country.create("GH", "GHA", "288", "가나", "Ghana", Continent.AFRICA),
                Country.create("KR", "KOR", "410", "대한민국", "South Korea", Continent.ASIA),
                Country.create("AQ", "ATA", "010", "남극", "Antarctica", Continent.ANTARCTICA)));

        List<CountryPublicResponse> result = countryService.getPublicList();

        assertThat(result).extracting(CountryPublicResponse::nameKo)
                .containsExactly("가나", "남극", "대한민국", "독일", "베트남");
    }
}
