package project.plantly.domain.company.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import project.plantly.domain.company.enums.CompanyVisibility;
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
        // 우편번호: 값이 오면 5자리 숫자(국가기초구역번호) 강제. null 은 통과 → 임시저장(DRAFT) 호환.
        @Pattern(regexp = "\\d{5}", message = "우편번호는 5자리 숫자여야 합니다.")
        String postalCode,  //우편번호
        String roadAddress,  //도로명 주소
        String jibunAddress,  //지번 주소(도로명만 있는 신주소는 비어있을 수 있음)
        String detailAddress,  //상세주소
        String website,  //기업홈페이지
        String logoUrl,  //로고 이미지
        String coverImageUrl,  //카드 커버 이미지(선택). 로고와 별개로 목록 카드 배경에 깔린다
        String introTitle,  //한줄요약
        String content,  //소개글
        TrlLevel trlLevel,  //기술성숙도
        String videoUrl,  //동영상링크
        String leadTime,  //예상 리드타임
        String asInfo,  //유지보수
        PricingType pricingType,  //견적 산출방식
        // 브랜드 컬러. 메인 스팟라이트 배너 배경 등 화면 CSS 에 그대로 꽂히는 값이라 형식을 강제한다.
        // null 은 통과(미지정) — 등록 시 blank 는 비울 대상이 없으므로 허용하지 않는다.
        @Pattern(regexp = BRAND_COLOR_PATTERN, message = BRAND_COLOR_MESSAGE)
        String brandColor,
        CompanyVisibility visibility,  //공개 범위(null = 공개 기본)

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
        // 인증만 평면 ID 가 아니다 — '기타' 를 고르면 인증명을 직접 적어 보내야 하고, 그건 마스터 ID 옆에
        // 붙어야 어느 항목의 이름인지가 정해진다. 일반 인증은 customName 없이 ID 만 담으면 된다.
        @Valid
        List<CertificationRequest> certifications,
        List<Long> countryIds,
        List<Long> domesticRegionIds,
        List<Long> industryIds
) {

    // 브랜드 컬러 형식(#RRGGBB). 등록·수정 세 DTO 가 같은 규칙을 써야 하므로 여기 한 곳에 둔다.
    // 화면이 이 값을 CSS 색상으로 그대로 사용하므로(메인 스팟라이트 배너 배경 등) 임의 문자열이 들어가면 안 된다.
    // 대소문자 헥사를 모두 받되 축약형(#RGB)·색 이름·rgb() 표기는 받지 않는다 — 저장 표기를 하나로 고정해
    // 프론트가 명도 계산(텍스트 대비 반전)을 분기 없이 할 수 있게 한다.
    public static final String BRAND_COLOR_PATTERN = "#[0-9a-fA-F]{6}";

    // 수정 경로 전용: 빈 문자열을 추가로 허용한다("" = 색 비우기, Company.updateBasicInfo 의 clear 규약).
    public static final String BRAND_COLOR_CLEARABLE_PATTERN = "|" + BRAND_COLOR_PATTERN;

    public static final String BRAND_COLOR_MESSAGE = "브랜드 컬러는 #RRGGBB 형식이어야 합니다.";

    public record ContactRequest(
            String contactName,
            String position,
            String phone,
            String email
    ) {
    }

    /**
     * 인증 1건 선택. 마스터 목록에서 고른 인증은 {@code certificationId} 만 담고,
     * 목록에 없어 '기타'를 고른 경우에만 {@code customName} 에 직접 입력한 인증명을 담는다.
     *
     * <p>둘의 짝은 링크 엔티티가 강제한다 — customName 이 있으면 마스터는 ETC 여야 하고, 없으면 아니어야 한다.
     * 같은 '기타' 마스터를 이름만 달리해 여러 건 보낼 수 있다(중복 판정 키가 ID 가 아니라 ID+이름이다).
     */
    public record CertificationRequest(
            @NotNull(message = "인증 ID는 필수입니다.")
            Long certificationId,
            @Size(max = 100, message = "직접 입력한 인증명은 100자를 넘을 수 없습니다.")
            String customName
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
