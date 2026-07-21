package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.entity.CompanyVerification;
import project.plantly.domain.company.entity.CompanyVerificationAttempt;
import project.plantly.domain.company.enums.VerificationOutcome;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.repository.CompanyRepository;
import project.plantly.domain.company.repository.CompanyVerificationAttemptRepository;
import project.plantly.domain.company.repository.CompanyVerificationRepository;
import project.plantly.global.exception.BusinessException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 인증 흐름의 DB 접근만 담당한다.
 *
 * <p>{@link CompanyVerificationService} 에서 분리한 이유는 트랜잭션 경계 때문이다. 국세청 호출은
 * 수백 ms~수 초가 걸리는데, 그걸 트랜잭션 안에서 하면 응답을 기다리는 내내 DB 커넥션을 붙잡는다.
 * 국세청이 느려지는 순간 커넥션 풀이 말라 서비스 전체가 멈춘다. 그래서 오케스트레이션(서비스)은
 * 트랜잭션 없이 두고, 짧은 DB 작업만 이 컴포넌트의 메서드 단위 트랜잭션으로 감싼다.
 *
 * <p>(같은 클래스 안에 두고 @Transactional 을 붙이면 자기 호출이라 프록시를 타지 않아 트랜잭션이
 * 아예 걸리지 않는다 — 별도 빈으로 나와야 하는 실질적인 이유이기도 하다.)
 */
@Component
@RequiredArgsConstructor
public class CompanyVerificationWriter {

    private final CompanyVerificationRepository verificationRepository;
    private final CompanyVerificationAttemptRepository attemptRepository;
    private final CompanyRepository companyRepository;

    /** 오늘 소진한 시도 횟수. 국세청 장애로 판정을 못 받은 건은 제외된다. */
    @Transactional(readOnly = true)
    public long countTodayAttempts(Long userId, LocalDateTime startOfDay) {
        return attemptRepository.countByUserIdAndCreatedAtAfterAndOutcomeNot(
                userId, startOfDay, VerificationOutcome.UNAVAILABLE);
    }

    /** 시도 1건을 감사 로그에 남긴다. 실패도 반드시 남긴다 — 재시도 제한 집계와 분쟁 근거가 여기서 나온다. */
    @Transactional
    public void recordAttempt(Long userId, String businessNumber, String ceoName, LocalDate businessStartDate,
                              VerificationOutcome outcome, String rawResponse) {
        attemptRepository.save(CompanyVerificationAttempt.of(
                userId, businessNumber, ceoName, businessStartDate, outcome, rawResponse));
    }

    /**
     * 중복 검사 후 인증 레코드를 발급한다.
     *
     * <p>중복 검사와 저장을 한 트랜잭션에 묶지만, 이것만으로 동시성이 완전히 막히지는 않는다. 인증 시점엔
     * 아직 회사가 없어서 두 사용자가 같은 번호로 동시에 인증하면 둘 다 통과할 수 있다. 최종 방어선은
     * 회사 등록 시점의 활성 부분 유니크 인덱스다({@code CompanyBusinessNumberIndexInitializer}).
     */
    @Transactional
    public CompanyVerification issue(Long userId, String businessNumber, String ceoName,
                                     LocalDate businessStartDate, String rawResponse,
                                     LocalDateTime now, Duration ttl) {
        if (companyRepository.existsByBusinessNumberAndDeletedFalse(businessNumber)) {
            attemptRepository.save(CompanyVerificationAttempt.of(
                    userId, businessNumber, ceoName, businessStartDate,
                    VerificationOutcome.DUPLICATE, rawResponse));
            throw new BusinessException(CompanyErrorCode.BUSINESS_NUMBER_TAKEN);
        }

        attemptRepository.save(CompanyVerificationAttempt.of(
                userId, businessNumber, ceoName, businessStartDate,
                VerificationOutcome.VALID, rawResponse));

        return verificationRepository.save(
                CompanyVerification.issue(userId, businessNumber, ceoName, businessStartDate, now, ttl));
    }
}
