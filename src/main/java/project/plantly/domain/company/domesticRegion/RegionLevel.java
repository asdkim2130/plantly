package project.plantly.domain.company.domesticRegion;

// 행정구역 단계. 시드(domestic-region.sql)의 level 값과 1:1로 매핑된다.
// NATION 과 REGION_GROUP 은 법정동코드에 대응 행이 없는 합성 단계다. 둘 다 parentCode 가 없어
// 시도와 나란히 루트로 나오며, 프론트의 "children 이 비면 확정" 규칙을 그대로 탄다.
public enum RegionLevel {
    NATION,        // 전국 — 합성 단계이며 시드에 단 한 행뿐이다.
    REGION_GROUP,  // 권역 — 여러 시도를 묶은 커버리지 단위(수도권). NATION 과 구분해야 전국만 특별 취급할 수 있다.
    SIDO,          // 시도 (특별시/광역시/도/특별자치시·도)
    SIGUNGU        // 시군구
}
