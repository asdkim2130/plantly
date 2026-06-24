package project.plantly.companyTest.support;

import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ImageRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ReferenceRequest;
import project.plantly.domain.company.enums.ImageType;

import java.util.List;

// CompanyCreateRequest 는 필드가 28개인 record 라, 테스트마다 전체 생성자를 호출하면 가독성이 떨어진다.
// 각 테스트가 "관심 있는 필드만" 세팅하도록 돕는 테스트 전용 빌더.
// 기본값은 정책을 건드리지 않는 무해한 값(대부분 null/빈 값)으로 둔다.
public class CompanyCreateRequestBuilder {

    // @NotBlank 대상은 의미 있는 기본값을 둔다. (정책 단위 테스트는 Bean Validation 을 거치지 않지만 현실성 유지)
    private String companyName = "테스트회사";
    private String ceoName = "홍길동";

    private String videoUrl;
    private String brandColor;
    private List<ImageRequest> images;
    private List<ReferenceRequest> references;
    private List<Long> categoryIds;

    public static CompanyCreateRequestBuilder aRequest() {
        return new CompanyCreateRequestBuilder();
    }

    public CompanyCreateRequestBuilder videoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
        return this;
    }

    public CompanyCreateRequestBuilder brandColor(String brandColor) {
        this.brandColor = brandColor;
        return this;
    }

    public CompanyCreateRequestBuilder categoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds;
        return this;
    }

    public CompanyCreateRequestBuilder images(List<ImageRequest> images) {
        this.images = images;
        return this;
    }

    public CompanyCreateRequestBuilder references(List<ReferenceRequest> references) {
        this.references = references;
        return this;
    }

    // n 장의 DETAIL 갤러리 이미지를 가진 요청을 만든다. (DetailImageLimitPolicy 용)
    public CompanyCreateRequestBuilder detailImages(int count) {
        this.images = imageUrls(count).stream()
                .map(url -> new ImageRequest(url, ImageType.DETAIL))
                .toList();
        return this;
    }

    // 단일 레퍼런스에 n 장의 이미지를 가진 요청을 만든다. (ReferenceImagePolicy 용)
    public CompanyCreateRequestBuilder referenceWithImages(int count) {
        this.references = List.of(new ReferenceRequest("프로젝트", null, null, null, imageUrls(count)));
        return this;
    }

    public CompanyCreateRequest build() {
        return new CompanyCreateRequest(
                null,            // businessNumber
                companyName,
                ceoName,
                null,            // establishmentDate
                null,            // postalCode
                null,            // address
                null,            // detailAddress
                null,            // website
                null,            // logoUrl
                null,            // introTitle
                null,            // content
                null,            // trlLevel
                videoUrl,
                null,            // leadTime
                null,            // asInfo
                null,            // pricingType
                brandColor,
                null,            // contacts
                images,
                references,
                null,            // materialNames
                null,            // equipmentNames
                null,            // tagNames
                categoryIds,
                null,            // certificationIds
                null,            // countryIds
                null,            // domesticRegionIds
                null             // industryIds
        );
    }

    private static List<String> imageUrls(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> "https://img/" + i + ".png")
                .toList();
    }
}
