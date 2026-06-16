package project.plantly.domain.company.domesticRegion;

// 행정구역 단계. 시드(domestic-region.sql)의 level 값과 1:1로 매핑된다.
public enum RegionLevel {
    SIDO,      // 시도 (특별시/광역시/도/특별자치시·도)
    SIGUNGU    // 시군구
}
