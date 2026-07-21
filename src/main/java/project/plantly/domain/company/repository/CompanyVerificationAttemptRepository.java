package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyVerificationAttempt;
import project.plantly.domain.company.enums.VerificationOutcome;

import java.time.LocalDateTime;

public interface CompanyVerificationAttemptRepository extends JpaRepository<CompanyVerificationAttempt, Long> {

    /**
     * 일일 재시도 제한 집계. 국세청 장애로 판정을 못 받은 시도(UNAVAILABLE)는 사용자 잘못이 아니므로 제외한다.
     */
    long countByUserIdAndCreatedAtAfterAndOutcomeNot(Long userId, LocalDateTime since, VerificationOutcome excluded);
}
