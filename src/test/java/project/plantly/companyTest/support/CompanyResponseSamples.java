package project.plantly.companyTest.support;

import project.plantly.domain.company.country.Continent;
import project.plantly.domain.company.domesticRegion.RegionLevel;
import project.plantly.domain.company.dto.CompanyDetailResponse;
import project.plantly.domain.company.dto.CompanyDetailResponse.ManagementMeta;
import project.plantly.domain.company.dto.CompanyPublicResponse;
import project.plantly.domain.company.dto.CompanyPublicResponse.CategoryResponse;
import project.plantly.domain.company.dto.CompanyPublicResponse.CertificationResponse;
import project.plantly.domain.company.dto.CompanyPublicResponse.ContactResponse;
import project.plantly.domain.company.dto.CompanyPublicResponse.CountryResponse;
import project.plantly.domain.company.dto.CompanyPublicResponse.GalleryImageResponse;
import project.plantly.domain.company.dto.CompanyPublicResponse.IndustryResponse;
import project.plantly.domain.company.dto.CompanyPublicResponse.ProjectReferenceResponse;
import project.plantly.domain.company.dto.CompanyPublicResponse.RegionResponse;
import project.plantly.domain.company.enums.ImageType;
import project.plantly.domain.company.enums.PricingType;
import project.plantly.domain.company.enums.RegistrationSource;
import project.plantly.domain.company.enums.TrlLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// 조회 컨트롤러 슬라이스 테스트(REST Docs)용 응답 샘플.
// 모든 필드를 채운(부속 포함) 표본이라 REST Docs 가 응답 트리 전체를 문서화할 수 있다.
public class CompanyResponseSamples {

    // 모든 필드가 채워진 공개 프로필 표본.
    public static CompanyPublicResponse fullPublic() {
        return new CompanyPublicResponse(
                1L, "플랜틀리테크", "김대표", LocalDate.of(2018, 3, 2),
                "06236", "서울 강남구 테헤란로 1", "10층",
                "https://plantly.test", "https://cdn.plantly.test/logo.png",
                "정밀 부품 전문", "정밀 가공 20년 경력의 부품 제조사입니다.",
                TrlLevel.MASS_PRODUCTION, "https://youtu.be/demo", "2주", "유선 AS 지원",
                PricingType.CONSULTATION, "#2E7D32",
                true, true, false,
                new ContactResponse("이담당", "영업팀장", "01022223333", "rep@plantly.test"),
                List.of(new GalleryImageResponse("https://cdn.plantly.test/gallery/1.png", ImageType.DETAIL, 0)),
                new ProjectReferenceResponse("스마트팩토리 구축", "불량률 30% 감소", "A제조", "2022-2023",
                        "https://cdn.plantly.test/ref/cover.png"),
                List.of("스테인리스"),
                List.of("CNC 선반"),
                List.of("정밀"),
                List.of(new CategoryResponse(1L, "정밀가공", "CAT-001", 1, "https://cdn.plantly.test/cat.png")),
                List.of(new CertificationResponse(1L, "ISO 9001")),
                List.of(new CountryResponse(1L, "KR", "대한민국", "South Korea", Continent.ASIA)),
                List.of(new RegionResponse(1L, "1100000000", "서울특별시", RegionLevel.SIDO)),
                List.of(new IndustryResponse(1L, "기계", "IND-001", "https://cdn.plantly.test/ind.png"))
        );
    }

    // 공개 프로필 + 내부·운영 메타가 모두 채워진 상세 표본.
    public static CompanyDetailResponse fullDetail() {
        return new CompanyDetailResponse(
                fullPublic(),
                new ManagementMeta(
                        "1234567890", RegistrationSource.USER, 7L, 7L,
                        true, 0, true, true, false, false,
                        LocalDateTime.of(2024, 1, 2, 3, 4, 5),
                        LocalDateTime.of(2024, 1, 2, 3, 4, 5)));
    }
}
