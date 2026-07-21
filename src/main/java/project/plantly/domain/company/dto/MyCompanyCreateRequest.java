package project.plantly.domain.company.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import project.plantly.domain.company.dto.CompanyCreateRequest.ContactRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ImageRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ReferenceRequest;
import project.plantly.domain.company.entity.CompanyVerification;
import project.plantly.domain.company.enums.CompanyVisibility;
import project.plantly.domain.company.enums.PricingType;
import project.plantly.domain.company.enums.TrlLevel;

import java.util.List;

/**
 * 유저 자가등록 요청.
 *
 * <p>관리자 등록({@link CompanyCreateRequest})과 DTO 를 나눈 이유는 <b>신원 3종(사업자번호·대표자명·개업일자)을
 * 아예 받지 않기 위해서</b>다. 이 값들은 선행 인증({@code POST /api/v1/companies/verification})에서 국세청
 * 검증을 통과한 것만 서버가 채운다. 요청 본문에 자리 자체가 없으니 "인증은 A 로 받고 저장은 B 로 하는"
 * 조작이 성립하지 않는다.
 *
 * <p>한 DTO 를 공유하면서 서버가 조용히 무시하는 방식도 가능하지만, 사용자가 보낸 대표자명이 흔적 없이
 * 버려져 디버깅이 어려워지고, ceoName 의 @NotBlank 를 풀어야 해서 관리자 경로의 검증까지 같이 약해진다.
 */
public record MyCompanyCreateRequest(
        // ===== 선행 인증 =====
        // POST /api/v1/companies/verification 이 발급한 식별자. 본인이 받은 것이어야 하고, 미사용·미만료여야 한다.
        @NotNull
        Long verificationId,

        // ===== 본체 (신원 3종 제외) =====
        @NotBlank
        String companyName,
        @Pattern(regexp = "\\d{5}", message = "우편번호는 5자리 숫자여야 합니다.")
        String postalCode,
        String roadAddress,
        String jibunAddress,
        String detailAddress,
        String website,
        String logoUrl,
        String introTitle,
        String content,
        TrlLevel trlLevel,
        String videoUrl,
        String leadTime,
        String asInfo,
        PricingType pricingType,
        String brandColor,
        CompanyVisibility visibility,

        // ===== 자식(소유) 엔티티 =====
        @Valid
        @Size(max = 1, message = "연락처는 현재 1건만 등록할 수 있습니다.")
        List<ContactRequest> contacts,
        @Valid
        List<ImageRequest> images,
        @Valid
        @Size(max = 1, message = "프로젝트 레퍼런스는 현재 1건만 등록할 수 있습니다.")
        List<ReferenceRequest> references,
        List<String> materialNames,
        List<String> equipmentNames,
        List<String> tagNames,

        // ===== 링크(M:N) 엔티티 =====
        List<Long> categoryIds,
        List<Long> certificationIds,
        List<Long> countryIds,
        List<Long> domesticRegionIds,
        List<Long> industryIds
) {

    /**
     * 검증된 신원 3종을 채워 공통 등록 요청으로 바꾼다. 등록 파이프라인(정책·자식·링크·검색 동기화)은
     * 두 경로가 공유하므로, 차이를 이 한 곳에서만 흡수한다.
     *
     * <p>개업일자를 establishmentDate 슬롯에 넣는다 — 별도 컬럼을 만들면 카드·검색·응답이 매번
     * "어느 날짜를 보여줄지" 분기해야 한다. 인증 여부(businessVerified)로 값의 출처가 이미 구분된다.
     */
    public CompanyCreateRequest toCreateRequest(CompanyVerification verification) {
        return new CompanyCreateRequest(
                verification.getBusinessNumber(),
                companyName,
                verification.getCeoName(),
                verification.getBusinessStartDate(),
                postalCode, roadAddress, jibunAddress, detailAddress,
                website, logoUrl, introTitle, content, trlLevel, videoUrl, leadTime, asInfo,
                pricingType, brandColor, visibility,
                contacts, images, references, materialNames, equipmentNames, tagNames,
                categoryIds, certificationIds, countryIds, domesticRegionIds, industryIds);
    }
}
