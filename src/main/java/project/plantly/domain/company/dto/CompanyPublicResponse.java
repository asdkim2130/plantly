package project.plantly.domain.company.dto;

import project.plantly.domain.company.category.Category;
import project.plantly.domain.company.certification.Certification;
import project.plantly.domain.company.country.Continent;
import project.plantly.domain.company.country.Country;
import project.plantly.domain.company.domesticRegion.DomesticRegion;
import project.plantly.domain.company.domesticRegion.RegionLevel;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanyContact;
import project.plantly.domain.company.entity.CompanyImage;
import project.plantly.domain.company.entity.CompanyProjectReference;
import project.plantly.domain.company.enums.ImageType;
import project.plantly.domain.company.enums.PricingType;
import project.plantly.domain.company.enums.TrlLevel;
import project.plantly.domain.company.industry.Industry;

import java.time.LocalDate;
import java.util.List;

// 일반(비소유자) 회사 상세 조회 응답. 누구에게 보여도 안전한 공개 필드만 담는다.
// 의도적으로 제외: businessNumber(사업자번호), 소유/등록 provenance(userId/registrationSource/registeredBy),
//                spotlightOrder, deleted, 타임스탬프 등 내부·운영 정보.
//                → 이들은 소유자/관리자 전용인 CompanyDetailResponse 의 meta 로만 노출한다.
//
// 부속(연락처/이미지/레퍼런스/소재·장비·태그/링크 마스터)은 민감 정보가 아니므로 공개 뷰에도 그대로 포함하며,
// 소유자/관리자 뷰는 이 응답을 'profile' 로 통째로 재사용한다.
public record CompanyPublicResponse(
        Long id,
        String companyName,
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
        String videoUrl,
        String leadTime,
        String asInfo,
        PricingType pricingType,
        String brandColor,

        // 공개 노출용 배지성 플래그 (노출 순서값 spotlightOrder 는 내부 정보라 제외)
        boolean verified,
        boolean featured,
        boolean spotlight,

        // ===== 부속 =====
        // 연락처/레퍼런스는 초기 버전에서 대표 1건만 노출한다(없으면 null). 전체 목록은 추후 '더보기' 전용 조회로 분리.
        ContactResponse representativeContact,
        List<GalleryImageResponse> galleryImages,
        ProjectReferenceResponse representativeReference,
        List<String> materialNames,
        List<String> equipmentNames,
        List<String> tagNames,
        List<CategoryResponse> categories,
        List<CertificationResponse> certifications,
        List<CountryResponse> countries,
        List<RegionResponse> regions,
        List<IndustryResponse> industries
) {

    public static CompanyPublicResponse from(CompanyAggregate aggregate) {
        Company c = aggregate.company();
        return new CompanyPublicResponse(
                c.getId(),
                c.getCompanyName(),
                c.getCeoName(),
                c.getEstablishmentDate(),
                c.getPostalCode(),
                c.getAddress(),
                c.getDetailAddress(),
                c.getWebsite(),
                c.getLogoUrl(),
                c.getIntroTitle(),
                c.getContent(),
                c.getTrlLevel(),
                c.getVideoUrl(),
                c.getLeadTime(),
                c.getAsInfo(),
                c.getPricingType(),
                c.getBrandColor(),
                c.isVerified(),
                c.isFeatured(),
                c.isSpotlight(),
                aggregate.representativeContact() == null ? null
                        : ContactResponse.from(aggregate.representativeContact()),
                aggregate.galleryImages().stream().map(GalleryImageResponse::from).toList(),
                aggregate.representativeReference() == null ? null
                        : ProjectReferenceResponse.from(aggregate.representativeReference(),
                                aggregate.representativeReferenceImages()),
                aggregate.materials().stream().map(m -> m.getMaterialName()).toList(),
                aggregate.equipment().stream().map(e -> e.getEquipmentName()).toList(),
                aggregate.tags().stream().map(t -> t.getTagName()).toList(),
                aggregate.categories().stream().map(CategoryResponse::from).toList(),
                aggregate.certifications().stream().map(CertificationResponse::from).toList(),
                aggregate.countries().stream().map(CountryResponse::from).toList(),
                aggregate.regions().stream().map(RegionResponse::from).toList(),
                aggregate.industries().stream().map(IndustryResponse::from).toList()
        );
    }

    public record ContactResponse(String contactName, String position, String phone, String email) {
        public static ContactResponse from(CompanyContact c) {
            return new ContactResponse(c.getContactName(), c.getPosition(), c.getPhone(), c.getEmail());
        }
    }

    public record GalleryImageResponse(String imageUrl, ImageType imageType, int displayOrder) {
        public static GalleryImageResponse from(CompanyImage image) {
            return new GalleryImageResponse(image.getImageUrl(), image.getImageType(), image.getDisplayOrder());
        }
    }

    // 프로젝트 레퍼런스 1건 + 그에 딸린 이미지 URL(순서대로).
    public record ProjectReferenceResponse(
            String projectTitle,
            String achievements,
            String partners,
            String period,
            List<String> imageUrls
    ) {
        public static ProjectReferenceResponse from(CompanyProjectReference ref, List<CompanyImage> images) {
            return new ProjectReferenceResponse(
                    ref.getProjectTitle(),
                    ref.getAchievements(),
                    ref.getPartners(),
                    ref.getPeriod(),
                    images.stream().map(CompanyImage::getImageUrl).toList());
        }
    }

    public record CategoryResponse(Long id, String categoryName, String categoryCode, int depth, String iconUrl) {
        public static CategoryResponse from(Category category) {
            return new CategoryResponse(category.getId(), category.getCategoryName(),
                    category.getCategoryCode(), category.getDepth(), category.getIconUrl());
        }
    }

    public record CertificationResponse(Long id, String certificationName) {
        public static CertificationResponse from(Certification certification) {
            return new CertificationResponse(certification.getId(), certification.getCertificationName());
        }
    }

    public record CountryResponse(Long id, String code, String nameKo, String nameEn, Continent continent) {
        public static CountryResponse from(Country country) {
            return new CountryResponse(country.getId(), country.getCode(),
                    country.getNameKo(), country.getNameEn(), country.getContinent());
        }
    }

    public record RegionResponse(Long id, String code, String name, RegionLevel level) {
        public static RegionResponse from(DomesticRegion region) {
            return new RegionResponse(region.getId(), region.getCode(), region.getName(), region.getLevel());
        }
    }

    public record IndustryResponse(Long id, String industryName, String industryCode, String iconUrl) {
        public static IndustryResponse from(Industry industry) {
            return new IndustryResponse(industry.getId(), industry.getIndustryName(),
                    industry.getIndustryCode(), industry.getIconUrl());
        }
    }
}
