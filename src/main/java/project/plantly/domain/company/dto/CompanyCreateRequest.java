package project.plantly.domain.company.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
        String businessNumber,  //사업자번호
        @NotBlank
        String companyName,  //기업이름
        @NotBlank
        String ceoName,  //대표자
        LocalDate establishmentDate,  //설립일
        String postalCode,  //우편번호
        String address,  //주소
        String detailAddress,  //상세주소
        String website,  //기업홈페이지
        String logoUrl,  //로고 이미지
        String introTitle,  //한줄요약
        String content,  //소개글
        TrlLevel trlLevel,  //기술성숙도
        String videoUrl,  //동영상링크
        String leadTime,  //예상 리드타임
        String asInfo,  //유지보수
        PricingType pricingType,  //견적 산출방식
        String brandColor,  //브랜드 컬러

        // ===== 자식(소유) 엔티티 =====
        // 연락처/레퍼런스는 하위 필드를 가진 컬렉션이라, 초기 버전은 대표 1건만 받는다.
        // (추후 다건 허용 + '더보기' 별도 조회로 확장 시 이 @Size 제약을 푼다)
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

    // 프로젝트 레퍼런스 1건 + 그에 딸린 이미지 URL 목록.
    // displayOrder(레퍼런스 순서/이미지 순서)는 클라이언트가 보내지 않고 서버가 리스트 인덱스로 부여한다.
    public record ReferenceRequest(
            String projectTitle,
            String achievements,
            String partners,
            String period,
            List<String> imageUrls
    ) {
    }
}
