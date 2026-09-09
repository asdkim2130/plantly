package project.plantly.domain.company.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import project.plantly.domain.company.enums.VerificationOutcome;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 인증 시도 1건의 감사 기록. 성공·실패를 가리지 않고 남긴다.
 *
 * <p>두 가지 용도가 있다. (1) 일일 재시도 제한 집계, (2) 분쟁 대응 — 나중에 혜택이 인증에 걸리면
 * "언제 어떤 입력으로 무슨 응답을 받아 통과시켰는가"를 재구성할 수 있어야 한다. 그래서 국세청
 * 원본 응답을 그대로 보관한다.
 */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class CompanyVerificationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long userId;

    @NotNull
    @Column(nullable = false)
    private String businessNumber;

    private String ceoName;

    private LocalDate businessStartDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationOutcome outcome;

    // 국세청 원본 응답(JSON). 장애로 판정 자체를 못한 경우엔 null.
    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    /**
     * 사용자가 직접 누른 시도가 아니라 서버가 대신 건 호출인지.
     *
     * <p>발행 시점에 만료된 인증을 자동으로 다시 물어보는 경로(refreshIfExpired)가 여기 해당한다.
     * 감사 기록으로서는 사용자 시도와 똑같이 남겨야 하지만(무슨 근거로 통과시켰는지 재구성해야 하므로),
     * <b>일일 시도 한도에서는 빠진다</b> — 사용자가 인지할 수 없는 호출로 한도를 깎으면
     * "아무것도 안 했는데 오늘 인증을 다 썼다" 가 성립한다.
     */
    @Column(nullable = false)
    private boolean automatic;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private CompanyVerificationAttempt(Long userId, String businessNumber, String ceoName, LocalDate businessStartDate,
                                       VerificationOutcome outcome, String rawResponse, boolean automatic) {
        this.userId = userId;
        this.businessNumber = businessNumber;
        this.ceoName = ceoName;
        this.businessStartDate = businessStartDate;
        this.outcome = outcome;
        this.rawResponse = rawResponse;
        this.automatic = automatic;
    }

    /** 사용자가 직접 누른 시도. 일일 한도를 소모한다. */
    public static CompanyVerificationAttempt of(Long userId, String businessNumber, String ceoName,
                                                LocalDate businessStartDate, VerificationOutcome outcome,
                                                String rawResponse) {
        return new CompanyVerificationAttempt(
                userId, businessNumber, ceoName, businessStartDate, outcome, rawResponse, false);
    }

    /** 서버가 대신 건 호출(발행 시점 자동 재질의). 감사 기록으로는 남고 일일 한도는 소모하지 않는다. */
    public static CompanyVerificationAttempt automatic(Long userId, String businessNumber, String ceoName,
                                                       LocalDate businessStartDate, VerificationOutcome outcome,
                                                       String rawResponse) {
        return new CompanyVerificationAttempt(
                userId, businessNumber, ceoName, businessStartDate, outcome, rawResponse, true);
    }
}
