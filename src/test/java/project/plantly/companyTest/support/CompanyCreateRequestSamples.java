package project.plantly.companyTest.support;

import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ContactRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ImageRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ReferenceRequest;
import project.plantly.domain.company.enums.ImageType;
import project.plantly.domain.company.enums.PricingType;
import project.plantly.domain.company.enums.TrlLevel;

import java.time.LocalDate;
import java.util.List;

// 컨트롤러 슬라이스(REST Docs) 문서화를 위한, 모든 필드를 채운 대표 요청.
// 서비스는 @MockitoBean 이라 정책 검증을 타지 않으므로 값은 문서 가독성 위주로 구성한다.
public class CompanyCreateRequestSamples {

    public static CompanyCreateRequest full() {
        return new CompanyCreateRequest(
                "123-45-67890",
                "플랜틀리",
                "김대표",
                LocalDate.of(2020, 1, 15),
                "06236",
                "서울시 강남구 테헤란로 1",
                "10층",
                "https://plantly.example.com",
                "https://img.example.com/logo.png",
                "친환경 소재 전문 기업",
                "회사 상세 소개 내용입니다.",
                TrlLevel.MASS_PRODUCTION,
                "https://youtu.be/abcdefg",
                "2주",
                "납품 후 1년 무상 A/S",
                PricingType.CONSULTATION,
                "#2E7D32",
                List.of(new ContactRequest("이담당", "팀장", "010-1234-5678", "contact@plantly.example.com")),
                List.of(new ImageRequest("https://img.example.com/detail1.png", ImageType.DETAIL)),
                List.of(new ReferenceRequest("스마트팜 구축", "수율 30% 향상", "A사", "2023", List.of("https://img.example.com/ref1.png"))),
                List.of("재생 플라스틱"),
                List.of("사출 성형기"),
                List.of("친환경", "B2B"),
                List.of(1L),
                List.of(2L),
                List.of(3L),
                List.of(4L),
                List.of(5L)
        );
    }
}
