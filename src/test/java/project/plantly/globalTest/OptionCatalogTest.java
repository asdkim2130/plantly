package project.plantly.globalTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import project.plantly.domain.company.enums.PricingType;
import project.plantly.domain.company.enums.TrlLevel;
import project.plantly.global.meta.OptionCatalog;
import project.plantly.global.meta.dto.EnumOption;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 옵션 카탈로그의 불변식.
 *
 * <p>카탈로그는 코드가 거의 없는 대신 <b>조용히 틀릴 수 있는</b> 자리가 셋이다: 라벨을 안 채운 상수가
 * 섞이는 것, 새 상수를 추가했는데 응답에 안 나오는 것, 키를 요청 DTO 필드 이름과 다르게 적는 것.
 * 셋 다 컴파일은 통과하고 화면에서야 드러나므로 여기서 잠근다.
 */
@DisplayName("OptionCatalog: 고정 선택지 카탈로그")
class OptionCatalogTest {

    // 빈 라벨은 드롭다운에 빈 줄로 나간다 — 값을 고를 수는 있는데 무엇을 고르는지 안 보이는 상태다.
    @ParameterizedTest
    @EnumSource(OptionCatalog.class)
    @DisplayName("모든 선택지가 값과 라벨을 채우고 있다")
    void everyOptionHasValueAndLabel(OptionCatalog entry) {
        assertThat(entry.options()).isNotEmpty();
        assertThat(entry.options()).allSatisfy(option -> {
            assertThat(option.value()).isNotBlank();
            assertThat(option.label()).isNotBlank();
        });
    }

    // enum 에 상수를 추가하고 카탈로그를 잊는 실수를 막는다. values() 기반이라 지금은 자동으로 따라오지만,
    // 누군가 목록을 손으로 나열하도록 바꾸면 이 테스트가 먼저 깨진다.
    @ParameterizedTest
    @EnumSource(OptionCatalog.class)
    @DisplayName("enum 상수가 하나도 빠지지 않고 선언 순서 그대로 실린다")
    void coversEveryConstantInDeclarationOrder(OptionCatalog entry) {
        List<String> exposed = entry.options().stream().map(EnumOption::value).toList();

        Class<?> source = switch (entry) {
            case TRL_LEVEL -> TrlLevel.class;
            case PRICING_TYPE -> PricingType.class;
        };
        List<String> declared = java.util.Arrays.stream(source.getEnumConstants())
                .map(constant -> ((Enum<?>) constant).name())
                .toList();

        assertThat(exposed).containsExactlyElementsOf(declared);
    }

    // 키는 요청 DTO 의 필드 이름과 같아야 한다 — 프론트가 제약 조회의 field, 검증 실패의 errors[].field 와
    // 같은 이름으로 세 응답을 이어 붙이기 때문이다. 이름이 어긋나면 폼이 선택지를 못 찾는다.
    @Test
    @DisplayName("카탈로그 키가 자가등록 요청 DTO 의 필드 이름과 일치한다")
    void keysMatchRequestFieldNames() {
        List<String> componentNames =
                java.util.Arrays.stream(project.plantly.domain.company.dto.MyCompanyCreateRequest.class
                                .getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toList();

        assertThat(OptionCatalog.all().keySet()).allSatisfy(key ->
                assertThat(componentNames).contains(key));
    }

    @Test
    @DisplayName("응답 카탈로그는 enum 선언 순서를 유지한다 (드롭다운 노출 순서)")
    void catalogPreservesDeclarationOrder() {
        Map<String, List<EnumOption>> catalog = OptionCatalog.all();

        assertThat(catalog.keySet()).containsExactly("trlLevel", "pricingType");
        assertThat(catalog.get("trlLevel"))
                .extracting(EnumOption::value)
                .containsExactly("PROTOTYPE", "MASS_PRODUCTION", "GLOBAL_STANDARD");
        assertThat(catalog.get("trlLevel").get(0).label()).isEqualTo("프로토타입");
    }
}
