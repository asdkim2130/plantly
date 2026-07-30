package project.plantly.domain.company.country;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.country.dto.CountryPublicResponse;
import project.plantly.global.response.ApiResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    /**
     * 공개 국가 옵션 목록. 회사 등록/수정 폼의 수출국 다중 선택 소스로, 비로그인 사용자도 등록 폼 이전
     * 화면을 둘러보므로 인증을 요구하지 않는다(SecurityConfig 에서 permitAll).
     *
     * <p>250건을 평면으로 한 번에 내려준다. 대륙 → 국가 2단 선택은 프론트가 {@code continent} 로
     * group by 해서 그린다 — 대륙 라벨과 노출 순서가 표현 계층 관심사이기 때문이며, 평면이라
     * 대륙을 넘나드는 국가명 검색도 클라이언트에서 바로 된다.
     *
     * <p>산업/인증과 달리 검색 패싯은 아니다 — {@code CompanySearchCriteria} 에 countryIds 가 없다.
     * 현재 이 목록의 소비자는 등록/수정 폼의 {@code countryIds} 뿐이다.
     */
    @GetMapping("/api/v1/countries")
    public ApiResponse<List<CountryPublicResponse>> getPublicList() {

        return ApiResponse.success(countryService.getPublicList());
    }
}
