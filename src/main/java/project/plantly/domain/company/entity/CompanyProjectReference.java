package project.plantly.domain.company.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class CompanyProjectReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(columnDefinition = "TEXT")
    private String projectTitle;

    @Column(columnDefinition = "TEXT")
    private String achievements;

    private String partners;

    private String period;

    // 프로젝트 이미지는 CompanyImage 가 project_reference_id FK 로 이 레퍼런스를 가리킨다. (1:N)
    private int displayOrder;

    // 상세 조회에 노출되는 대표 레퍼런스 여부. 초기 버전은 회사당 1건만 등록되어 그 건이 대표가 된다.
    // (추후 다건 허용 + '더보기' 별도 조회로 확장 시, 대표 1건만 상세에 싣고 나머지는 별도 API 로 분리)
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean representative;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public CompanyProjectReference(Company company, String projectTitle, String achievements, String partners, String period, int displayOrder) {
        this.company = company;
        this.projectTitle = projectTitle;
        this.achievements = achievements;
        this.partners = partners;
        this.period = period;
        this.displayOrder = displayOrder;
    }

    // 상세 조회에 노출되는 대표 레퍼런스로 지정한다.
    public void markAsRepresentative() {
        this.representative = true;
    }
}
