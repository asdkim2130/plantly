package project.plantly.global.seed;

import project.plantly.domain.company.domesticRegion.DomesticRegion;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ContactRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ImageRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ReferenceRequest;
import project.plantly.domain.company.dto.MyCompanyCreateRequest;
import project.plantly.domain.company.enums.CompanyVisibility;
import project.plantly.domain.company.enums.ImageType;
import project.plantly.domain.company.enums.PricingType;
import project.plantly.domain.company.enums.TrlLevel;
import project.plantly.domain.company.policy.GradePolicy;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 등록 요청 조립기. {@link CompanyCreateRequest} 는 필드가 30개라 그대로 쓰면 케이스 정의가 읽히지 않는다.
 *
 * <p>기본값은 "평범하게 잘 채워진 회사"이고, 각 케이스는 자기가 증명하려는 축만 덮어쓴다. 그래야 케이스
 * 목록을 읽을 때 무엇이 그 케이스의 본질인지가 한눈에 보인다.
 *
 * <p>{@link #limitTo(GradePolicy)} 는 등급 한도에 맞춰 컬렉션 크기를 줄인다. 자가등록(FREE) 경로는 정책이
 * 실제로 발화하므로 이걸 안 맞추면 등록 자체가 거부된다. 관리자 등록은 면제라 거부되진 않지만, 등급별
 * 화면 차이를 보려면 데이터도 그 등급처럼 보여야 하므로 동일하게 맞춘다.
 */
public class SeedCompanyRequestBuilder {

    private final int index;

    private String businessNumber;
    private String companyName;
    private String ceoName;
    private LocalDate establishmentDate;
    private String postalCode;
    private String roadAddress;
    private String jibunAddress;
    private String detailAddress;
    private String website;
    private String logoUrl;
    private String introTitle;
    private String content;
    private TrlLevel trlLevel;
    private String videoUrl;
    private String leadTime;
    private String asInfo;
    private PricingType pricingType;
    private String brandColor;
    private CompanyVisibility visibility = CompanyVisibility.PUBLIC;

    private List<ContactRequest> contacts;
    private List<ImageRequest> images;
    private List<ReferenceRequest> references;
    private List<String> materialNames;
    private List<String> equipmentNames;
    private List<String> tagNames;

    private List<Long> categoryIds;
    private List<Long> certificationIds;
    private List<Long> countryIds;
    private List<Long> domesticRegionIds;
    private List<Long> industryIds;

    private SeedCompanyRequestBuilder(int index, SeedMasterCatalog masters) {
        this.index = index;
        DomesticRegion region = masters.region(index);
        // 주소 문자열은 "시도 시군구" 로 시작해야 카드 지역 라벨이 온전히 잘린다. 링크는 region 그대로.
        String addressRegionName = masters.addressRegionName(region, index);

        this.companyName = SeedVocabulary.companyName(index);
        this.ceoName = SeedVocabulary.ceoName(index);
        this.establishmentDate = SeedVocabulary.establishmentDate(index);
        this.postalCode = SeedVocabulary.postalCode(index);
        this.roadAddress = SeedVocabulary.roadAddress(addressRegionName, index);
        this.jibunAddress = SeedVocabulary.jibunAddress(addressRegionName, index);
        this.detailAddress = SeedVocabulary.detailAddress(index);
        this.website = SeedVocabulary.website(index);
        this.logoUrl = SeedVocabulary.logoUrl(index);
        this.introTitle = SeedVocabulary.introTitle(index);
        this.content = SeedVocabulary.content(index);
        this.trlLevel = TrlLevel.values()[Math.floorMod(index, TrlLevel.values().length)];
        this.videoUrl = null; // 등급이 허용할 때만 limitTo 가 채운다
        this.leadTime = SeedVocabulary.leadTime(index);
        this.asInfo = SeedVocabulary.asInfo(index);
        this.pricingType = PricingType.values()[Math.floorMod(index, PricingType.values().length)];
        this.brandColor = null; // 등급이 허용할 때만 limitTo 가 채운다

        this.contacts = List.of(new ContactRequest(
                SeedVocabulary.contactName(index), "영업팀장",
                SeedVocabulary.phone(index), SeedVocabulary.email(index)));
        this.images = detailImages(3);
        this.references = List.of(reference(0));
        this.materialNames = SeedVocabulary.materials(index, 3);
        this.equipmentNames = SeedVocabulary.equipment(index, 3);
        this.tagNames = SeedVocabulary.tags(index, 3);

        this.categoryIds = masters.categoryIds(index, 1);
        this.certificationIds = masters.certificationIds(index, 2);
        this.countryIds = masters.countryIds(index, 2);
        this.domesticRegionIds = List.of(region.getId());
        this.industryIds = masters.industryIds(index, 1);
    }

    public static SeedCompanyRequestBuilder of(int index, SeedMasterCatalog masters) {
        return new SeedCompanyRequestBuilder(index, masters);
    }

    /**
     * 등급 한도에 맞춰 컬렉션·선택 필드를 채운다. 카테고리는 상한만큼 넉넉히 붙여서, 등급이 올라가면
     * 카드에 더 많은 배지가 보인다는 사실 자체가 화면으로 드러나게 한다.
     */
    public SeedCompanyRequestBuilder limitTo(GradePolicy policy, SeedMasterCatalog masters) {
        this.categoryIds = masters.categoryIds(index, policy.maxCompanyCategories());
        this.images = detailImages(Math.min(policy.maxDetailImages(), 6));
        this.videoUrl = policy.videoAllowed() ? SeedVocabulary.videoUrl(index) : null;
        this.brandColor = policy.customBrandColorAllowed() ? SeedVocabulary.brandColor(index) : null;
        this.references = List.of(reference(Math.min(policy.maxReferenceImages(), 4)));
        return this;
    }

    public SeedCompanyRequestBuilder businessNumber(String value) {
        this.businessNumber = value;
        return this;
    }

    public SeedCompanyRequestBuilder companyName(String value) {
        this.companyName = value;
        return this;
    }

    public SeedCompanyRequestBuilder ceoName(String value) {
        this.ceoName = value;
        return this;
    }

    public SeedCompanyRequestBuilder visibility(CompanyVisibility value) {
        this.visibility = value;
        return this;
    }

    public SeedCompanyRequestBuilder content(String value) {
        this.content = value;
        return this;
    }

    public SeedCompanyRequestBuilder introTitle(String value) {
        this.introTitle = value;
        return this;
    }

    public SeedCompanyRequestBuilder categoryIds(List<Long> value) {
        this.categoryIds = value;
        return this;
    }

    public SeedCompanyRequestBuilder certificationIds(List<Long> value) {
        this.certificationIds = value;
        return this;
    }

    public SeedCompanyRequestBuilder countryIds(List<Long> value) {
        this.countryIds = value;
        return this;
    }

    public SeedCompanyRequestBuilder industryIds(List<Long> value) {
        this.industryIds = value;
        return this;
    }

    public SeedCompanyRequestBuilder domesticRegionIds(List<Long> value) {
        this.domesticRegionIds = value;
        return this;
    }

    public SeedCompanyRequestBuilder equipmentNames(List<String> value) {
        this.equipmentNames = value;
        return this;
    }

    public SeedCompanyRequestBuilder detailImageCount(int count) {
        this.images = detailImages(count);
        return this;
    }

    public SeedCompanyRequestBuilder referenceImageCount(int count) {
        this.references = List.of(reference(count));
        return this;
    }

    /** 모든 부속 컬렉션을 비운다. 빈 섹션이 어떻게 그려지는지 확인하는 케이스용. */
    public SeedCompanyRequestBuilder withoutCollections() {
        this.contacts = List.of();
        this.images = List.of();
        this.references = List.of();
        this.materialNames = List.of();
        this.equipmentNames = List.of();
        this.tagNames = List.of();
        return this;
    }

    /** 선택 필드를 전부 비운다. null 자리에 프론트가 무엇을 그리는지 확인하는 케이스용. */
    public SeedCompanyRequestBuilder withoutOptionalFields() {
        // 지번은 선택 필드다. 지번이 없는 주소에서 우편번호 서비스가 내려주는 빈 문자열을 그대로 재현한다
        // — Address 가 이를 null 로 접는지(= 상세 화면에 빈 줄이 남지 않는지)까지 시드로 드러난다.
        this.jibunAddress = "";
        this.website = null;
        this.introTitle = null;
        this.content = null;
        this.trlLevel = null;
        this.videoUrl = null;
        this.leadTime = null;
        this.asInfo = null;
        this.pricingType = null;
        this.brandColor = null;
        this.establishmentDate = null;
        return this;
    }

    public CompanyCreateRequest build() {
        return new CompanyCreateRequest(
                businessNumber, companyName, ceoName, establishmentDate,
                postalCode, roadAddress, jibunAddress, detailAddress,
                website, logoUrl, introTitle, content, trlLevel, videoUrl, leadTime, asInfo,
                pricingType, brandColor, visibility,
                contacts, images, references, materialNames, equipmentNames, tagNames,
                categoryIds, certificationIds, countryIds, domesticRegionIds, industryIds);
    }

    /**
     * 자가등록 요청. 신원 3종(사업자번호·대표자명·개업일자)은 자리 자체가 없다 — 선행 인증 레코드에서만 온다.
     */
    public MyCompanyCreateRequest buildMy(Long verificationId) {
        return new MyCompanyCreateRequest(
                verificationId, companyName,
                postalCode, roadAddress, jibunAddress, detailAddress,
                website, logoUrl, introTitle, content, trlLevel, videoUrl, leadTime, asInfo,
                pricingType, brandColor, visibility,
                contacts, images, references, materialNames, equipmentNames, tagNames,
                categoryIds, certificationIds, countryIds, domesticRegionIds, industryIds);
    }

    // 회사 직속 갤러리는 DETAIL 타입만 허용된다(GalleryImageTypePolicy). PROJECT 이미지는 레퍼런스로만 들어간다.
    private List<ImageRequest> detailImages(int count) {
        return IntStream.range(0, Math.max(count, 0))
                .mapToObj(slot -> new ImageRequest(SeedVocabulary.imageUrl(index, slot), ImageType.DETAIL))
                .toList();
    }

    private ReferenceRequest reference(int imageCount) {
        List<String> imageUrls = IntStream.range(0, Math.max(imageCount, 0))
                .mapToObj(slot -> SeedVocabulary.imageUrl(index, 100 + slot))
                .toList();
        return new ReferenceRequest(
                SeedVocabulary.projectTitle(index),
                SeedVocabulary.projectAchievements(index),
                SeedVocabulary.projectPartners(index),
                SeedVocabulary.projectPeriod(index),
                imageUrls);
    }
}
