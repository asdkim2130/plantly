package project.plantly.domain.company.entity.link;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import project.plantly.domain.company.domesticRegion.DomesticRegion;
import project.plantly.domain.company.entity.Company;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "domestic_region_id"}))
public class CompanyDomesticRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domestic_region_id", nullable = false)
    private DomesticRegion domesticRegion;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CompanyDomesticRegion(Company company, DomesticRegion domesticRegion) {
        this.company = company;
        this.domesticRegion = domesticRegion;
    }
}
