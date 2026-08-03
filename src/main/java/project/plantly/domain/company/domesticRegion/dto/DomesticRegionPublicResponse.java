package project.plantly.domain.company.domesticRegion.dto;

import lombok.Builder;
import project.plantly.domain.company.domesticRegion.DomesticRegion;
import project.plantly.domain.company.domesticRegion.RegionLevel;

import java.util.List;

/**
 * 공개 지역 옵션 트리 응답 — 등록/수정 폼의 국내 커버리지 선택 소스.
 *
 * <p>선택 UI 는 두 단계다. 1차에서 이 목록의 루트(전국 + 시도)를 고르고,
 * {@code children} 이 비어 있으면 그대로 확정, 비어 있지 않으면 2차 드롭다운이 열린다.
 * 2차의 첫 항목 '전역'에는 <b>부모 자신의 id</b> 를 바인딩한다 — 시도 행 자체가
 * "그 시도 전역"을 뜻하므로 별도의 전역 행이 없다. ('경기 전역' = 경기도 행)
 *
 * <p>그래서 요청으로 돌아오는 값은 단계와 무관하게 언제나 지역 id 하나다.
 *
 * <p>{@code shortName} 은 드롭다운 항목용(부모명 없음), {@code displayName} 은 확정 후
 * 배지용(부모명 포함) 이다. 프론트가 조합할 필요 없이 그대로 쓰면 된다.
 */
@Builder
public record DomesticRegionPublicResponse(
        Long id,
        String shortName,
        String displayName,
        RegionLevel level,
        List<DomesticRegionPublicResponse> children
) {
    public static DomesticRegionPublicResponse of(DomesticRegion region, List<DomesticRegionPublicResponse> children) {
        return DomesticRegionPublicResponse.builder()
                .id(region.getId())
                .shortName(region.getShortName())
                .displayName(region.getDisplayName())
                .level(region.getLevel())
                .children(children)
                .build();
    }
}
