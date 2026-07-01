package project.plantly.domain.company.dto;

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
public record CompanyUpdateRequest(
        @Size(min = 1, message = "기업명은 빈 값으로 변경할 수 없습니다.")
        String companyName,
        @Size(min = 1, message = "대표자명은 빈 값으로 변경할 수 없습니다.")
        String ceoName,
        LocalDate establishmentDate,
        @Size(min = 1, message = "우편번호는 빈 값으로 변경할 수 없습니다.")
        String postalCode,
        @Size(min = 1, message = "주소는 빈 값으로 변경할 수 없습니다.")
        String address,
        @Size(min = 1, message = "상세주소는 빈 값으로 변경할 수 없습니다.")
        String detailAddress,
        String website,
        @Size(min = 1, message = "로고 이미지는 빈 값으로 변경할 수 없습니다.")
        String logoUrl,
        String introTitle,
        String content,
        TrlLevel trlLevel,
        String videoUrl,
        String leadTime,
        String asInfo,
        PricingType pricingType,
        String brandColor
) {
}
