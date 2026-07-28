package project.plantly.domain.company.entity.link;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.industry.Industry;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 패싯 필터(industry_id IN (...) → company_id)는 유니크 제약 btree 의 선두 컬럼과 맞지 않아 전용 인덱스를 둔다.
// 상세 근거는 CompanyCertification 의 동일 인덱스 주석 참고.
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "industry_id"}),
        indexes = @Index(name = "idx_company_industry_facet", columnList = "industry_id, company_id"))
public class CompanyIndustry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_id", nullable = false)
    private Industry industry;

    // 회사가 등록 시 선택한 순서(요청 순서). 조회/노출은 이 순서를 따른다.
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int displayOrder;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CompanyIndustry(Company company, Industry industry, int displayOrder) {
        this.company = company;
        this.industry = industry;
        this.displayOrder = displayOrder;
    }
}
