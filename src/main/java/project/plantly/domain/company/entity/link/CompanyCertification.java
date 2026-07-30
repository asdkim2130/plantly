package project.plantly.domain.company.entity.link;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import project.plantly.domain.company.certification.Certification;
import project.plantly.domain.company.entity.Company;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 유니크 제약의 btree 는 (company_id, certification_id) 순이라 company_id 로 시작하는 조회만 seek 가능하다.
// 패싯 필터는 certification_id IN (...) → company_id 의 역방향 조회이므로 선두 컬럼이 맞지 않아
// 전용 인덱스를 둔다. company_id 를 포함시켜 heap 접근 없는 index-only scan 이 되게 한다.
// (검색 섬의 company_category_closure 가 idx_ccc_category 로 같은 처방을 해둔 것과 동일한 이유)
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "certification_id"}),
        indexes = @Index(name = "idx_company_cert_facet", columnList = "certification_id, company_id"))
public class CompanyCertification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certification_id", nullable = false)
    private Certification certification;

    // 회사가 등록 시 선택한 순서(요청 순서). 조회/노출은 이 순서를 따른다.
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int displayOrder;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CompanyCertification(Company company, Certification certification, int displayOrder) {
        this.company = company;
        this.certification = certification;
        this.displayOrder = displayOrder;
    }
}
