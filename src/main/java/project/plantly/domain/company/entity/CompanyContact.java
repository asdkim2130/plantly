package project.plantly.domain.company.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    private String contactName;

    private String position;

    private String phone;

    private String email;

    private int displayOrder;

    // 상세 조회에 노출되는 대표 연락처 여부. 초기 버전은 회사당 1건만 등록되어 그 건이 대표가 된다.
    // (추후 다건 허용 + '더보기' 별도 조회로 확장 시, 대표 1건만 상세에 싣고 나머지는 별도 API 로 분리)
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean representative;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;


    public CompanyContact(Company company, String contactName, String position, String phone, String email, int displayOrder) {
        this.company = company;
        this.contactName = contactName;
        this.position = position;
        this.phone = phone;
        this.email = email;
        this.displayOrder = displayOrder;
    }

    // 상세 조회에 노출되는 대표 연락처로 지정한다.
    public void markAsRepresentative() {
        this.representative = true;
    }
}
