package project.plantly.domain.company.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import project.plantly.domain.company.enums.PricingType;
import project.plantly.domain.company.enums.RegistrationSource;
import project.plantly.domain.company.enums.TrlLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소유 유저 식별자. 관리자가 대신 등록한 회사는 등록 시점에 소유자가 없을 수 있으므로 nullable.
    // 추후 관계자가 가입하면 assignOwner()로 연동한다. (도메인 결합 회피 위해 raw id로 참조)
    private Long userId;

    // 등록 경로(USER/ADMIN). 연동 정책 확정 전까지 누가 어떤 경로로 등록했는지 추적하는 provenance 용도.
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationSource registrationSource;

    // 등록 행위자 식별자. 유저 자가등록이면 본인 userId, 관리자 등록이면 admin id. (raw id로 참조)
    private Long registeredBy;

    // 사업자번호. 초안 단계 회사는 미입력 가능하므로 nullable. (Postgres는 UNIQUE 컬럼에 다중 NULL 허용)
    @Column(unique = true)
    private String businessNumber;

    @NotNull
    @Column(nullable = false)
    private String companyName;

    @NotNull
    @Column(nullable = false)
    private String ceoName;

    private LocalDate establishmentDate;

    @NotNull
    @Column(nullable = false)
    private String postalCode;

    @NotNull
    @Column(nullable = false)
    private String address;

    @NotNull
    @Column(nullable = false)
    private String detailAddress;

    private String website;

    @NotNull
    @Column(nullable = false)
    private String logoUrl;  // 기업 대표 이미지(단일·필수). 여러 장 이미지는 CompanyImage 로 분리 관리한다.

    private String introTitle;  // 한 줄 요약

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private TrlLevel trlLevel;

    private String videoUrl;

    private String leadTime;

    @Column(columnDefinition = "TEXT")
    private String asInfo;

    @Enumerated(EnumType.STRING)
    private PricingType pricingType;

    private String brandColor;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private boolean featured = false;

    @Column(nullable = false)
    private boolean spotlight = false;

    private int spotlightOrder;

    @Column(nullable = false)
    private boolean deleted = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 비즈니스 필드만 받는다. 시스템 관리 플래그(verified/featured/spotlight/spotlightOrder/deleted)는
    // 생성 시 기본값(false/0)으로 시작하고, 상태 전환은 도메인 행위 메서드로만 수행한다.
    private Company(Long userId, RegistrationSource registrationSource, Long registeredBy, String businessNumber, String companyName, String ceoName, LocalDate establishmentDate, String postalCode, String address, String detailAddress, String website, String logoUrl, String introTitle, String content, TrlLevel trlLevel, String videoUrl, String leadTime, String asInfo, PricingType pricingType, String brandColor) {
        this.userId = userId;
        this.registrationSource = registrationSource;
        this.registeredBy = registeredBy;
        this.businessNumber = businessNumber;
        this.companyName = companyName;
        this.ceoName = ceoName;
        this.establishmentDate = establishmentDate;
        this.postalCode = postalCode;
        this.address = address;
        this.detailAddress = detailAddress;
        this.website = website;
        this.logoUrl = logoUrl;
        this.introTitle = introTitle;
        this.content = content;
        this.trlLevel = trlLevel;
        this.videoUrl = videoUrl;
        this.leadTime = leadTime;
        this.asInfo = asInfo;
        this.pricingType = pricingType;
        this.brandColor = brandColor;
    }

    // 유저 자가등록: 등록 즉시 소유자 = 본인. registeredBy 도 본인.
    public static Company createByUser(Long userId, String businessNumber, String companyName, String ceoName, LocalDate establishmentDate, String postalCode, String address, String detailAddress, String website, String logoUrl, String introTitle, String content, TrlLevel trlLevel, String videoUrl, String leadTime, String asInfo, PricingType pricingType, String brandColor) {
        return new Company(userId, RegistrationSource.USER, userId, businessNumber, companyName, ceoName, establishmentDate, postalCode, address, detailAddress, website, logoUrl, introTitle, content, trlLevel, videoUrl, leadTime, asInfo, pricingType, brandColor);
    }

    // 관리자 등록: 소유자 미연동(userId=null) 상태로 시작. registeredBy 는 등록한 admin id.
    public static Company createByAdmin(Long adminId, String businessNumber, String companyName, String ceoName, LocalDate establishmentDate, String postalCode, String address, String detailAddress, String website, String logoUrl, String introTitle, String content, TrlLevel trlLevel,  String videoUrl, String leadTime, String asInfo, PricingType pricingType, String brandColor) {
        return new Company(null, RegistrationSource.ADMIN, adminId, businessNumber, companyName, ceoName, establishmentDate, postalCode, address, detailAddress, website, logoUrl, introTitle, content, trlLevel, videoUrl, leadTime, asInfo, pricingType, brandColor);
    }

    // ===== 기본 정보 부분 수정 =====
    // null = 미변경. 선택(nullable) 문자열 필드는 blank("") 을 받으면 비운다(null 로 clear).
    // 필수 필드의 blank 는 요청 DTO(@Size(min=1)) 에서 거르므로 여기선 null 여부만 본다.
    // 날짜·enum 은 blank 개념이 없어 clear 를 지원하지 않는다(값이 오면 교체만).
    // 시스템 플래그·사업자번호·등록 provenance(userId/registrationSource/registeredBy) 는 이 경로로 바꾸지 않는다.
    public void updateBasicInfo(String companyName, String ceoName, LocalDate establishmentDate,
                                String postalCode, String address, String detailAddress,
                                String website, String logoUrl, String introTitle, String content,
                                TrlLevel trlLevel, String videoUrl, String leadTime, String asInfo,
                                PricingType pricingType, String brandColor) {
        // 필수 필드: null = 미변경 (blank 는 DTO 에서 차단)
        if (companyName != null) this.companyName = companyName;
        if (ceoName != null) this.ceoName = ceoName;
        if (postalCode != null) this.postalCode = postalCode;
        if (address != null) this.address = address;
        if (detailAddress != null) this.detailAddress = detailAddress;
        if (logoUrl != null) this.logoUrl = logoUrl;

        // 선택 문자열 필드: null = 미변경, blank = 비움(null)
        if (website != null) this.website = blankToNull(website);
        if (introTitle != null) this.introTitle = blankToNull(introTitle);
        if (content != null) this.content = blankToNull(content);
        if (videoUrl != null) this.videoUrl = blankToNull(videoUrl);
        if (leadTime != null) this.leadTime = blankToNull(leadTime);
        if (asInfo != null) this.asInfo = blankToNull(asInfo);
        if (brandColor != null) this.brandColor = blankToNull(brandColor);

        // clear 미지원(날짜·enum): null = 미변경, 값 = 교체
        if (establishmentDate != null) this.establishmentDate = establishmentDate;
        if (trlLevel != null) this.trlLevel = trlLevel;
        if (pricingType != null) this.pricingType = pricingType;
    }

    private static String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }

    // ===== 상태 변경 (도메인 행위) =====

    // 소유자 연동. 관리자가 미연동(userId=null)으로 등록한 회사에 추후 관계자가 가입하면 호출한다.
    public void assignOwner(Long userId) {
        this.userId = userId;
    }

    // 소유자 연동 여부
    public boolean isClaimed() {
        return this.userId != null;
    }

    // 관리자 인증 처리 / 해제
    public void verify() {
        this.verified = true;
    }

    public void revokeVerification() {
        this.verified = false;
    }

    // 추천(featured) 노출 / 해제
    public void feature() {
        this.featured = true;
    }

    public void unfeature() {
        this.featured = false;
    }

    // 스팟라이트 노출. 노출 순서를 함께 지정한다.
    public void turnOnSpotlight(int spotlightOrder) {
        this.spotlight = true;
        this.spotlightOrder = spotlightOrder;
    }

    // 스팟라이트 해제. 순서값도 초기화한다.
    public void turnOffSpotlight() {
        this.spotlight = false;
        this.spotlightOrder = 0;
    }

    // 스팟라이트 노출 중 순서만 변경
    public void changeSpotlightOrder(int spotlightOrder) {
        this.spotlightOrder = spotlightOrder;
    }

    // 등급 정책에 따라 등록 시점 spotlight 를 활성화한다. (노출 순서는 별도 큐레이션 전까지 기본값 유지)
    public void activateSpotlight() {
        this.spotlight = true;
    }

    // 등급 정책에 따라 브랜드 컬러를 강제 지정한다. (커스텀 불가 등급의 기본값 고정 등)
    public void changeBrandColor(String brandColor) {
        this.brandColor = brandColor;
    }

    // 소프트 삭제 / 복구
    public void delete() {
        this.deleted = true;
    }

    public void restore() {
        this.deleted = false;
    }
}
