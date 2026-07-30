package project.plantly.companyTest.countryTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import project.plantly.domain.company.country.Continent;
import project.plantly.domain.company.country.Country;
import project.plantly.domain.company.country.CountryRepository;
import project.plantly.domain.company.country.CountryService;
import project.plantly.domain.company.country.dto.CountryPublicResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class CountryServiceTest {

    @Mock CountryRepository countryRepository;
    @InjectMocks CountryService countryService;

    @Test
    @DisplayName("공개 목록은 폼에 필요한 필드만 매핑한다")
    public void getPublicList_mapToDto (){
        Country korea = country(1L, "KR", "KOR", "410", "대한민국", "South Korea", Continent.ASIA);
        given(countryRepository.findAll()).willReturn(List.of(korea));

        List<CountryPublicResponse> result = countryService.getPublicList();

        assertThat(result).hasSize(1);
        CountryPublicResponse first = result.get(0);
        assertThat(first.id()).isEqualTo(1L);
        assertThat(first.code()).isEqualTo("KR");          // 국기 아이콘 렌더용 alpha-2
        assertThat(first.nameKo()).isEqualTo("대한민국");
        assertThat(first.nameEn()).isEqualTo("South Korea");
        assertThat(first.continent()).isEqualTo(Continent.ASIA);
    }

    @Test
    @DisplayName("정렬은 DB 가 아니라 서비스가 한다 — 리포지토리 순서와 무관하게 가나다순으로 나온다")
    public void getPublicList_sortsByKoreanName (){
        // 리포지토리가 임의 순서로 돌려줘도(= ORDER BY 없음, collation 영향 없음) 결과는 항상 같다.
        // Postgres 는 en_US.utf8 에서 "대한민국"을 "독일"보다 뒤에 놓는다 — 그 순서를 그대로
        // 흘려보내면 안 된다는 것이 이 테스트의 요지다.
        given(countryRepository.findAll()).willReturn(List.of(
                country(1L, "VN", "VNM", "704", "베트남", "Viet Nam", Continent.ASIA),
                country(2L, "DE", "DEU", "276", "독일", "Germany", Continent.EUROPE),
                country(3L, "GH", "GHA", "288", "가나", "Ghana", Continent.AFRICA),
                country(4L, "KR", "KOR", "410", "대한민국", "South Korea", Continent.ASIA),
                country(5L, "AQ", "ATA", "010", "남극", "Antarctica", Continent.ANTARCTICA)));

        List<CountryPublicResponse> result = countryService.getPublicList();

        assertThat(result).extracting(CountryPublicResponse::nameKo)
                .containsExactly("가나", "남극", "대한민국", "독일", "베트남");
    }

    @Test
    @DisplayName("남극·무인 속령도 목록에서 빼지 않는다 (Country 에는 active 가 없다)")
    public void getPublicList_includesUninhabitedTerritories (){
        Country korea = country(1L, "KR", "KOR", "410", "대한민국", "South Korea", Continent.ASIA);
        Country antarctica = country(2L, "AQ", "ATA", "010", "남극", "Antarctica", Continent.ANTARCTICA);
        Country bouvet = country(3L, "BV", "BVT", "074", "부베 섬", "Bouvet Island", Continent.ANTARCTICA);
        given(countryRepository.findAll()).willReturn(List.of(korea, antarctica, bouvet));

        List<CountryPublicResponse> result = countryService.getPublicList();

        // ISO 마스터는 폐기 개념이 없어 걸러낼 근거(active)가 엔티티에 없다. 수출국으로 고를 일이
        // 없다는 것만으로 서버가 임의로 빼면, 빼는 기준이 코드에만 남는 암묵 규칙이 된다.
        assertThat(result).hasSize(3);
        assertThat(result).extracting(CountryPublicResponse::code)
                .containsExactly("AQ", "KR", "BV");        // 가나다순: 남극 < 대한민국 < 부베 섬
        assertThat(result).extracting(CountryPublicResponse::continent)
                .contains(Continent.ANTARCTICA);
    }

    @Test
    @DisplayName("국가가 없으면 빈 리스트를 반환")
    public void getPublicList_empty (){
        given(countryRepository.findAll()).willReturn(List.of());

        assertThat(countryService.getPublicList()).isEmpty();
    }

    // 테스트 헬퍼 — id 는 @GeneratedValue 라 생성자로 못 넣는다.
    private Country country (Long id, String code, String alpha3, String numericCode,
                             String nameKo, String nameEn, Continent continent){
        Country country = Country.create(code, alpha3, numericCode, nameKo, nameEn, continent);
        ReflectionTestUtils.setField(country, "id", id);
        return country;
    }
}
