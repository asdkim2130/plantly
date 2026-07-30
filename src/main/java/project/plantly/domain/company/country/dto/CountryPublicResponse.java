package project.plantly.domain.company.country.dto;

import project.plantly.domain.company.country.Continent;
import project.plantly.domain.company.country.Country;

/**
 * 공개 국가 옵션. 회사 등록/수정 폼의 수출국 다중 선택이 쓴다.
 *
 * <p>250건 전체를 평면으로 한 번에 내려주고, 대륙 → 국가 2단 선택은 프론트가 {@code continent} 로
 * 묶어서 그린다. 서버가 미리 대륙별로 묶지 않는 이유는 그룹 라벨("아시아")·대륙 노출 순서가 모두
 * 표현 계층 관심사이기 때문이다 ({@code CertificationPublicResponse} 가 {@code type} 을 다루는 방식과 동일).
 * 평면이라 대륙을 넘나드는 국가명 타이핑 검색도 클라이언트에서 왕복 없이 된다.
 *
 * <p>250건이라 페이로드가 커 보이지만 행당 약 100B, 전체 25KB 수준이고 gzip 후 5~6KB다.
 * ISO 마스터라 사실상 불변이어서 캐시 적중률도 높다.
 *
 * <p>{@code alpha3}/{@code numericCode} 는 노출하지 않는다 — 드롭다운 라벨에도 검색에도 쓰이지 않는
 * 식별자다. {@code code}(alpha-2)는 남긴다: 국기 아이콘/이모지를 클라이언트가 코드로 렌더할 수 있다.
 * 다른 마스터와 달리 운영 필드(displayOrder/active) 자체가 엔티티에 없다 — ISO 고정 목록이라
 * 폐기 개념이 없기 때문이다.
 */
public record CountryPublicResponse(
        Long id,
        String code,
        String nameKo,
        String nameEn,
        Continent continent
) {

    public static CountryPublicResponse from(Country country) {
        return new CountryPublicResponse(
                country.getId(),
                country.getCode(),
                country.getNameKo(),
                country.getNameEn(),
                country.getContinent());
    }
}
