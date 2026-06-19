package project.plantly.domain.company.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import project.plantly.domain.company.enums.ImageType;
import project.plantly.domain.company.enums.PricingType;
import project.plantly.domain.company.enums.TrlLevel;

import java.time.LocalDate;
import java.util.List;

// 회사 등록 요청. 본체 비즈니스 필드 + 부속 10종(자식 5 / 링크 5)을 한 번에 받는다.
// 소유자(userId)/등록 경로(source)는 요청 본문이 아니라 진입점(유저 인증 principal / admin 권한)에서 결정하므로
// 이 DTO에는 담지 않는다.
//
// 검증 정책: 엔티티 NOT NULL 제약과 일치하는 최소한(companyName/ceoName)만 강제한다.
// "발행 시점에만 필수"인 나머지 항목은 임시저장(DRAFT) 호환을 위해 여기서 막지 않고
// 추후 publish 단계 검증으로 분리한다.
//
// 부속 리스트의 표시 순서(displayOrder)는 클라이언트가 보내지 않고, 서비스에서 리스트 인덱스로 부여한다.
public record CompanyCreateRequest(
        // ===== 본체 =====
        String businessNumber,
        @NotBlank
        String companyName,
        @NotBlank
        String ceoName,
        LocalDate establishmentDate,
        String postalCode,
        String address,
        String detailAddress,
        String website,
        String logoUrl,
        String introTitle,
        String content,
        TrlLevel trlLevel,
        String projectTitle,
        String achievements,
        String partners,
        String videoUrl,
        String leadTime,
        String asInfo,
        PricingType pricingType,
        String brandColor,

        // ===== 자식(소유) 엔티티 =====
        @Valid
        List<ContactRequest> contacts,
        @Valid
        List<ImageRequest> images,
        List<String> materialNames,
        List<String> equipmentNames,
        List<String> tagNames,

        // ===== 링크(M:N) 엔티티 - 기존 마스터 ID 참조 =====
        List<Long> categoryIds,
        List<Long> certificationIds,
        List<Long> countryIds,
        List<Long> domesticRegionIds,
        List<Long> industryIds
) {

    public record ContactRequest(
            String contactName,
            String position,
            String phone,
            String email
    ) {
    }

    public record ImageRequest(
            String imageUrl,
            ImageType imageType
    ) {
    }
}
