package project.plantly.domain.company.domesticRegion.dto;

import lombok.Builder;
import project.plantly.domain.company.domesticRegion.DomesticRegion;

import java.util.List;

// 관리자용 행정구역 트리 응답. 지역 코드는 노출하지 않고 name/active 와 children 만 내려준다.
@Builder
public record DomesticRegionAdminResponse(
        String name,
        boolean active,
        List<DomesticRegionAdminResponse> children
) {
    public static DomesticRegionAdminResponse of(DomesticRegion region, List<DomesticRegionAdminResponse> children) {
        return DomesticRegionAdminResponse.builder()
                .name(region.getName())
                .active(region.isActive())
                .children(children)
                .build();
    }
}
