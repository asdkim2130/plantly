package project.plantly.domain.company.entity.link;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import project.plantly.domain.company.category.Category;
import project.plantly.domain.company.entity.Company;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "category_id"}))
public class CompanyCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // 회사가 등록 시 선택한 순서(요청 순서). 조회/노출은 이 순서를 따른다.
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int displayOrder;

    // 공개 노출 여부. false = 저장은 살아 있지만 공개 조회·카드·검색 색인에서 빠진 상태.
    // 등급 한도를 초과한 링크를 '삭제하지 않고 가리는' 무손실 재조정의 표현이며, 등급이 오르면 다시 켜서 복구한다.
    // 조회는 이 플래그만 읽는다 — 왜 꺼졌는지(등급/만료)는 쓰기·배치 쪽이 알고, 읽기 경로로 새지 않는다.
    // 지금은 항상 true 로 생성된다. 이 값을 계산해 내리는 주체(만료 재조정 배치)는 요금제와 함께 붙는다.
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CompanyCategory(Company company, Category category, int displayOrder) {
        this.company = company;
        this.category = category;
        this.displayOrder = displayOrder;
    }

    // 노출 on/off. 방향을 명시하는 멱등 연산이라 토글이 아니다(회사 삭제/복구와 같은 방식).
    public void changeActive(boolean active) {
        this.active = active;
    }
}
