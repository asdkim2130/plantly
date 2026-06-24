package project.plantly.domain.company.dto;

import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.enums.RegistrationSource;

import java.time.LocalDateTime;

// 소유자/관리자 전용 회사 상세 조회 응답.
// 공개 필드는 CompanyPublicResponse(profile)를 통째로 재사용하고, 내부·운영 정보만 meta 로 덧붙인다.
// (공개 응답과 필드 중복 없이 '공개 + 내부' 를 합성으로 표현 → 공개 경계가 구조로 드러난다)
//
// 확장 지점: 회사 등급/플랜 잔여기간은 아직 회사 레벨로 모델링되지 않아 지금은 담지 않는다.
//           추후 Subscription/Membership 이 생기면 profile/meta 와 나란히 별도 컴포넌트(예: membership)로 추가한다.
public record CompanyDetailResponse(
        CompanyPublicResponse profile,
        ManagementMeta meta
) {

    public static CompanyDetailResponse from(CompanyAggregate aggregate) {
        return new CompanyDetailResponse(
                CompanyPublicResponse.from(aggregate),
                ManagementMeta.from(aggregate.company()));
    }

    // 소유자/관리자에게만 보이는 내부·운영 메타데이터.
    public record ManagementMeta(
            String businessNumber,
            RegistrationSource registrationSource,
            Long registeredBy,
            Long ownerUserId,
            boolean claimed,        // 소유자 연동 여부 (관리자 등록 후 미연동이면 false)
            int spotlightOrder,
            boolean verified,
            boolean featured,
            boolean spotlight,
            boolean deleted,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static ManagementMeta from(Company c) {
            return new ManagementMeta(
                    c.getBusinessNumber(),
                    c.getRegistrationSource(),
                    c.getRegisteredBy(),
                    c.getUserId(),
                    c.isClaimed(),
                    c.getSpotlightOrder(),
                    c.isVerified(),
                    c.isFeatured(),
                    c.isSpotlight(),
                    c.isDeleted(),
                    c.getCreatedAt(),
                    c.getUpdatedAt());
        }
    }
}
