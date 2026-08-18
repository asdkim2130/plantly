package project.plantly.domain.company.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import project.plantly.domain.company.enums.ImageType;

import java.time.LocalDateTime;

// 회사의 '여러 장' 이미지를 한 테이블에서 통합 관리한다.
// (단일 필수 이미지인 logoUrl 은 Company 스칼라 컬럼으로 별도 보관 — NOT NULL 보장을 위해)
// - 모든 이미지는 회사 소속이므로 company 는 NOT NULL.
// - 회사 직속 갤러리(상세 이미지) 는 projectReference == null.
// - 프로젝트 레퍼런스 이미지는 projectReference 가 채워진다. (CompanyProjectReference 입장에선 1:N)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // 프로젝트 레퍼런스에 속한 이미지면 연결, 회사 직속 갤러리 이미지면 null.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_reference_id")
    private CompanyProjectReference projectReference;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private ImageType imageType;

    private int displayOrder;

    // 공개 노출 여부. false = 저장은 살아 있지만 공개 조회에서 빠진 상태(CompanyCategory.active 와 같은 규약).
    // 갤러리 상세 이미지(DETAIL)와 레퍼런스 이미지 둘 다 등급 한도가 걸려 있어 같은 테이블의 한 플래그로 다룬다 —
    // 다만 세는 단위가 다르다(DETAIL 은 회사당, 레퍼런스 이미지는 레퍼런스 1건당). 그 차이는 값을 계산하는
    // 재조정 배치가 흡수하고, 조회는 단위와 무관하게 이 플래그만 읽는다.
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private CompanyImage(Company company, CompanyProjectReference projectReference, String imageUrl, ImageType imageType, int displayOrder) {
        this.company = company;
        this.projectReference = projectReference;
        this.imageUrl = imageUrl;
        this.imageType = imageType;
        this.displayOrder = displayOrder;
    }

    // 회사 직속 갤러리 이미지 (projectReference 없음). imageType 으로 갤러리 종류를 구분한다.
    public static CompanyImage ofCompany(Company company, String imageUrl, ImageType imageType, int displayOrder) {
        return new CompanyImage(company, null, imageUrl, imageType, displayOrder);
    }

    // 프로젝트 레퍼런스 이미지. 소속 회사는 레퍼런스의 회사를 그대로 따라가 항상 일치시킨다.
    public static CompanyImage ofProject(CompanyProjectReference projectReference, String imageUrl, int displayOrder) {
        return new CompanyImage(projectReference.getCompany(), projectReference, imageUrl, ImageType.PROJECT, displayOrder);
    }

    // 노출 on/off. 방향을 명시하는 멱등 연산이라 토글이 아니다.
    public void changeActive(boolean active) {
        this.active = active;
    }
}