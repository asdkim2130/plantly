package project.plantly.companyTest.support;

import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.CertificationRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ContactRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ImageRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ReferenceRequest;
import project.plantly.domain.company.dto.CompanyReverificationRequest;
import project.plantly.domain.company.dto.CompanyVerificationRequest;
import project.plantly.domain.company.dto.MyCompanyCreateRequest;
import project.plantly.domain.company.enums.CompanyVisibility;
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
                "서울시 강남구 역삼동 736-1",
                "10층",
                "https://plantly.example.com",
                "https://img.example.com/logo.png",
                "https://img.example.com/cover.png",
                "친환경 소재 전문 기업",
                "회사 상세 소개 내용입니다.",
                TrlLevel.MASS_PRODUCTION,
                "https://youtu.be/abcdefg",
                "2주",
                "납품 후 1년 무상 A/S",
                PricingType.CONSULTATION,
                "#2E7D32",
                CompanyVisibility.PUBLIC,
                List.of(new ContactRequest("이담당", "팀장", "010-1234-5678", "contact@plantly.example.com")),
                List.of(new ImageRequest("https://img.example.com/detail1.png", ImageType.DETAIL)),
                List.of(new ReferenceRequest("스마트팜 구축", "수율 30% 향상", "A사", "2023", List.of("https://img.example.com/ref1.png"))),
                List.of("재생 플라스틱"),
                List.of("사출 성형기"),
                List.of("친환경", "B2B"),
                List.of(1L),
                // 인증 2건 — 마스터에서 고른 것 1건 + '기타'(9L) 에 직접 적어 넣은 것 1건.
                List.of(new CertificationRequest(2L, null), new CertificationRequest(9L, "사내 표준 품질인증 QM-2024")),
                List.of(3L),
                List.of(4L),
                List.of(5L)
        );
    }

    // 자가등록 요청 표본. 신원 3종(사업자번호/대표자명/개업일자) 자리가 없고 verificationId 만 있다는 점이
    // 관리자 등록 요청과의 유일한 구조적 차이다 — 그 값들은 선행 인증에서만 온다.
    public static MyCompanyCreateRequest myFull() {
        return new MyCompanyCreateRequest(
                99L,
                "플랜틀리",
                "06236",
                "서울시 강남구 테헤란로 1",
                "서울시 강남구 역삼동 736-1",
                "10층",
                "https://plantly.example.com",
                "https://img.example.com/logo.png",
                "https://img.example.com/cover.png",
                "친환경 소재 전문 기업",
                "회사 상세 소개 내용입니다.",
                TrlLevel.MASS_PRODUCTION,
                "https://youtu.be/abcdefg",
                "2주",
                "납품 후 1년 무상 A/S",
                PricingType.CONSULTATION,
                "#2E7D32",
                CompanyVisibility.PUBLIC,
                List.of(new ContactRequest("이담당", "팀장", "010-1234-5678", "contact@plantly.example.com")),
                List.of(new ImageRequest("https://img.example.com/detail1.png", ImageType.DETAIL)),
                List.of(new ReferenceRequest("스마트팜 구축", "수율 30% 향상", "A사", "2023", List.of("https://img.example.com/ref1.png"))),
                List.of("재생 플라스틱"),
                List.of("사출 성형기"),
                List.of("친환경", "B2B"),
                List.of(1L),
                // 인증 2건 — 마스터에서 고른 것 1건 + '기타'(9L) 에 직접 적어 넣은 것 1건.
                List.of(new CertificationRequest(2L, null), new CertificationRequest(9L, "사내 표준 품질인증 QM-2024")),
                List.of(3L),
                List.of(4L),
                List.of(5L)
        );
    }

    // 사업자 인증 요청 표본. 사업자번호는 하이픈 포함으로 둔다 — 서버 정규화가 문서에서도 드러나도록.
    public static CompanyVerificationRequest verificationRequest() {
        return new CompanyVerificationRequest("123-45-67890", "김대표", LocalDate.of(2020, 1, 15));
    }

    // 사업자 재인증 요청 표본. 사업자번호 자리가 없다 — 저장된 번호로만 국세청에 재질의한다(탈취 방지).
    public static CompanyReverificationRequest reverificationRequest() {
        return new CompanyReverificationRequest("김신임", LocalDate.of(2021, 3, 4));
    }
}
