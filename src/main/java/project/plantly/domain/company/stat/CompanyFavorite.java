package project.plantly.domain.company.stat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// 유저의 회사 즐겨찾기(목록으로 관리). 해제 = 행 삭제(hard delete).
// User↔Company cross-aggregate 연결이므로 raw id로 느슨하게 참조한다. (@ManyToOne 미사용 — CompanyMember 규칙)
// (userId, companyId) 유일 — 같은 회사 중복 즐겨찾기 불가.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "company_id"}))
public class CompanyFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long companyId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CompanyFavorite(Long userId, Long companyId) {
        this.userId = userId;
        this.companyId = companyId;
    }
}
