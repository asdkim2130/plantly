package project.plantly.domain.company.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 회사의 구독. 구독의 주인은 회사(Company 1:1) 이며, 정책이 참조하는 등급의 유일한 출처다.
// raw id(companyId) 로 느슨하게 참조한다. (Company.userId / CompanyMember 와 동일 철학 — @ManyToOne 으로 묶지 않는다)
//
// 저장하는 건 "무엇을(grade) 어떤 상태로(status) 언제부터(startedAt) 언제까지(expiresAt)" 라는 '사실' 뿐이다.
// 정책이 실제로 참조하는 '지금 유효한 등급' 은 저장하지 않고 effectiveGrade() 로 파생한다.
// → 구독을 연장하지 않으면 별도 잡/데이터 변경 없이 만료일이 지나는 순간 자동으로 FREE 로 강등된다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "company_id")) // 회사당 구독 1건(1:1)
public class CompanySubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 구독의 주인(회사) 식별자. 등록 트랜잭션 안에서 회사 저장으로 id 를 확보한 뒤 assignCompany() 로 연결한다.
    @Column(nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyGrade grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private LocalDate startedAt;

    // null = 무기한(만료 없음). FREE·ADMIN_EXEMPT 는 무기한으로 둔다.
    private LocalDate expiresAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private CompanySubscription(Long companyId, CompanyGrade grade, SubscriptionStatus status, LocalDate startedAt, LocalDate expiresAt) {
        this.companyId = companyId;
        this.grade = grade;
        this.status = status;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }

    // 유저 자가등록 회사: FREE, 무기한. (상위 혜택은 추후 구독/보상으로 부여)
    // companyId 는 회사 저장 후 assignCompany() 로 채운다.
    public static CompanySubscription freeForUser(LocalDate startedAt) {
        return active(CompanyGrade.FREE, startedAt, null);
    }

    // 유료 활성 구독. 업그레이드/구독 갱신 등에서 임의 등급의 활성 구독을 만든다.
    // expiresAt == null 이면 무기한.
    public static CompanySubscription active(CompanyGrade grade, LocalDate startedAt, LocalDate expiresAt) {
        return new CompanySubscription(null, grade, SubscriptionStatus.ACTIVE, startedAt, expiresAt);
    }

    // 체험 구독. 등급 혜택을 받되 expiresAt 만료 시 effectiveGrade 가 FREE 로 강등된다.
    public static CompanySubscription trial(CompanyGrade grade, LocalDate startedAt, LocalDate expiresAt) {
        return new CompanySubscription(null, grade, SubscriptionStatus.TRIAL, startedAt, expiresAt);
    }

    // 관리자 등록 회사: 등급 한도 정책 면제(ADMIN_EXEMPT). grade 는 명목상 ENTERPRISE(면제 해제 시 기본치), 무기한.
    public static CompanySubscription adminExempt(LocalDate startedAt) {
        return new CompanySubscription(null, CompanyGrade.ENTERPRISE, SubscriptionStatus.ADMIN_EXEMPT, startedAt, null);
    }

    // 회사 저장으로 id 를 확보한 뒤 1:1 로 연결한다. (등록 트랜잭션 내 1회 호출)
    public void assignCompany(Long companyId) {
        this.companyId = companyId;
    }

    // 등급 한도 정책 면제 여부(관리자 등록). 구조 검증 정책은 면제하지 않는다.
    public boolean isExempt() {
        return status == SubscriptionStatus.ADMIN_EXEMPT;
    }

    // 만료 여부. expiresAt == null 은 무기한(만료 없음)이라 항상 미만료.
    public boolean isExpired(LocalDate asOf) {
        return expiresAt != null && expiresAt.isBefore(asOf);
    }

    // 지금(asOf 기준) 유효한 등급. 저장하지 않고 파생한다:
    //  - 면제(ADMIN_EXEMPT): 저장 grade 를 그대로 사용(정책 자체가 면제 처리하므로 값은 명목상).
    //  - 만료된 유료 구독: FREE 로 강등(연장 안 하면 자연히 FREE).
    //  - 그 외(활성/체험 중): 저장 grade.
    public CompanyGrade effectiveGrade(LocalDate asOf) {
        if (isExempt()) {
            return grade;
        }
        if (isExpired(asOf)) {
            return CompanyGrade.FREE;
        }
        return grade;
    }

    public CompanyGrade effectiveGrade() {
        return effectiveGrade(LocalDate.now());
    }
}
