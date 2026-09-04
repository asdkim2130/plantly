package project.plantly.domain.company.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import project.plantly.domain.company.enums.PricingType;
import project.plantly.domain.company.enums.TrlLevel;

import java.time.LocalDate;

// 회사 기본 정보 부분 수정(PATCH) 요청. 모든 필드가 nullable 이며 null = 미변경(sparse update).
// 수정 가능한 본체 스칼라만 담는다 — 컬렉션(태그/링크/이미지 등)은 별도 PUT 엔드포인트로 교체하고,
// 시스템 플래그(verified/featured/spotlight/spotlightOrder/deleted)·사업자번호·등록 provenance 는
// 이 DTO 에 담지 않는 것으로 접근을 차단한다.
//
// clear(비우기) 규약: 선택 문자열 필드는 blank("") 를 보내면 비운다(null). null 은 미변경.
// 필수 필드(NOT NULL)는 blank 로 비울 수 없으므로 @Size(min = 1) 로 빈 문자열만 거른다(null 은 통과 = 미변경).
// 날짜·enum 은 blank 개념이 없어 clear 를 지원하지 않는다(값이 오면 교체만).
//
// 길이 상한은 등록 경로와 같은 값을 쓴다(CompanyConstraints). 등록에서만 막으면 같은 값이 수정으로 들어온다.
public record CompanyUpdateRequest(
        @Size(min = 1, max = CompanyConstraints.COMPANY_NAME_MAX,
                message = "기업명은 1자 이상 100자 이하여야 합니다.")
        String companyName,
        @Size(min = 1, max = CompanyConstraints.CEO_NAME_MAX,
                message = "대표자명은 1자 이상 50자 이하여야 합니다.")
        String ceoName,
        @PastOrPresent(message = "설립일은 오늘보다 미래일 수 없습니다.")
        LocalDate establishmentDate,
        // null = 미변경. 값이 오면 5자리 숫자 강제 — blank("") 도 불일치라 거절(= 빈 값으로 못 비움, 기존 @Size(min=1) 포섭).
        @Pattern(regexp = CompanyConstraints.POSTAL_CODE_PATTERN, message = CompanyConstraints.POSTAL_CODE_MESSAGE)
        String postalCode,
        @Size(min = 1, max = CompanyConstraints.ROAD_ADDRESS_MAX,
                message = "도로명 주소는 1자 이상 200자 이하여야 합니다.")
        String roadAddress,
        // 지번은 선택 필드라 blank("") 로 비울 수 있다(null = 미변경). 그래서 @Size(min=1) 를 걸지 않는다.
        @Size(max = CompanyConstraints.JIBUN_ADDRESS_MAX, message = "지번 주소는 200자를 넘을 수 없습니다.")
        String jibunAddress,
        @Size(min = 1, max = CompanyConstraints.DETAIL_ADDRESS_MAX,
                message = "상세주소는 1자 이상 100자 이하여야 합니다.")
        String detailAddress,
        @Size(max = CompanyConstraints.URL_MAX, message = "홈페이지 주소는 255자를 넘을 수 없습니다.")
        String website,
        // 로고도 선택 필드가 되면서 @Size(min=1) 를 뗐다 — blank("") 로 비울 수 있고, 비면 프론트가
        // 회사명 앞 두 글자로 대체 배지를 그린다. (엔티티의 NOT NULL 도 함께 풀렸다)
        @Size(max = CompanyConstraints.URL_MAX, message = "로고 이미지 주소는 255자를 넘을 수 없습니다.")
        String logoUrl,
        // 커버는 선택 필드라 blank("") 로 비울 수 있다(null = 미변경).
        @Size(max = CompanyConstraints.URL_MAX, message = "커버 이미지 주소는 255자를 넘을 수 없습니다.")
        String coverImageUrl,
        @Size(max = CompanyConstraints.INTRO_TITLE_MAX, message = "한 줄 요약은 50자를 넘을 수 없습니다.")
        String introTitle,
        @Size(max = CompanyConstraints.CONTENT_MAX, message = "소개글은 5000자를 넘을 수 없습니다.")
        String content,
        TrlLevel trlLevel,
        @Size(max = CompanyConstraints.URL_MAX, message = "동영상 주소는 255자를 넘을 수 없습니다.")
        String videoUrl,
        @Size(max = CompanyConstraints.LEAD_TIME_MAX, message = "예상 리드타임은 100자를 넘을 수 없습니다.")
        String leadTime,
        @Size(max = CompanyConstraints.AS_INFO_MAX, message = "유지보수 정보는 1000자를 넘을 수 없습니다.")
        String asInfo,
        PricingType pricingType,
        // null = 미변경, "" = 색 비우기, 그 외에는 #RRGGBB 만 허용한다.
        // 우편번호와 달리 blank 를 패턴에 포함시킨 이유는 브랜드 컬러가 선택 필드라 비울 수 있어야 하기 때문이다.
        @Pattern(regexp = CompanyConstraints.BRAND_COLOR_CLEARABLE_PATTERN,
                message = CompanyConstraints.BRAND_COLOR_MESSAGE)
        String brandColor
) {
}
