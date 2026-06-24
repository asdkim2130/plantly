package project.plantly.companyTest.support;

import project.plantly.domain.company.entity.Company;

// 변형 정책(BrandColorPolicy / SpotlightPolicy) 테스트는 mutate 대상 Company 인스턴스가 필요하다.
// 정책이 읽거나 바꾸는 필드(brandColor, spotlight)만 의미 있게 두고 나머지는 null 로 둔 유저 등록 회사를 만든다.
public class CompanyFixture {

    public static Company userCompany() {
        return userCompanyWithBrandColor(null);
    }

    public static Company userCompanyWithBrandColor(String brandColor) {
        return Company.createByUser(
                1L,              // userId
                null,            // businessNumber
                "테스트회사",      // companyName
                "홍길동",          // ceoName
                null, null, null, null, null, null, null, null, null,  // ~ trlLevel
                null,            // videoUrl
                null, null, null,// leadTime, asInfo, pricingType
                brandColor);
    }
}
