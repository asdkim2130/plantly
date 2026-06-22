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
}
