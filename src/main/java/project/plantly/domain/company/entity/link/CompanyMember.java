package project.plantly.domain.company.entity.link;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import project.plantly.domain.company.enums.MemberRole;

import java.time.LocalDateTime;

// 회사-유저 멤버십. User↔Company N:M 관계가 사는 곳(association table)이며,
// 두 애그리거트를 raw id로 느슨하게 참조한다. (Company.userId 와 동일 철학 — @ManyToOne 으로 객체 그래프를 묶지 않는다)
// 현재는 회사 등록자를 OWNER 로 1건 기록하는 최소 구조만 둔다. (초대/수락/권한분화는 추후)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "user_id"})) // 같은 유저 중복 합류 차단
public class CompanyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private CompanyMember(Long companyId, Long userId, MemberRole role) {
        this.companyId = companyId;
        this.userId = userId;
        this.role = role;
    }

    // 회사 생성자(자가등록 유저)를 소유자로 등록한다.
    public static CompanyMember owner(Long companyId, Long userId) {
        return new CompanyMember(companyId, userId, MemberRole.OWNER);
    }
}