package project.plantly.domain.company.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import project.plantly.domain.company.enums.PricingType;
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

    // 소유 유저 식별자. 회사는 반드시 소유 유저를 가진다. (도메인 결합 회피 위해 raw id로 참조)
    @Column(nullable = false)
    private Long userId;

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

    private String postalCode;

    private String address;

    private String detailAddress;

    private String website;

    private String logoUrl;

    private String introTitle;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private TrlLevel trlLevel;

    private String projectTitle;

    @Column(columnDefinition = "TEXT")
    private String achievements;

    @Column(columnDefinition = "TEXT")
    private String partners;

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
    private Company(Long userId, String businessNumber, String companyName, String ceoName, LocalDate establishmentDate, String postalCode, String address, String detailAddress, String website, String logoUrl, String introTitle, String content, TrlLevel trlLevel, String projectTitle, String achievements, String partners, String videoUrl, String leadTime, String asInfo, PricingType pricingType, String brandColor) {
        this.userId = userId;
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
        this.projectTitle = projectTitle;
        this.achievements = achievements;
        this.partners = partners;
        this.videoUrl = videoUrl;
        this.leadTime = leadTime;
        this.asInfo = asInfo;
        this.pricingType = pricingType;
        this.brandColor = brandColor;
    }

    public static Company create(Long userId, String businessNumber, String companyName, String ceoName, LocalDate establishmentDate, String postalCode, String address, String detailAddress, String website, String logoUrl, String introTitle, String content, TrlLevel trlLevel, String projectTitle, String achievements, String partners, String videoUrl, String leadTime, String asInfo, PricingType pricingType, String brandColor) {
        return new Company(userId, businessNumber, companyName, ceoName, establishmentDate, postalCode, address, detailAddress, website, logoUrl, introTitle, content, trlLevel, projectTitle, achievements, partners, videoUrl, leadTime, asInfo, pricingType, brandColor);
    }

    // ===== 상태 변경 (도메인 행위) =====

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

    // 소프트 삭제 / 복구
    public void delete() {
        this.deleted = true;
    }

    public void restore() {
        this.deleted = false;
    }
}
