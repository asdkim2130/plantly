package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyDraft;

import java.util.Optional;

public interface CompanyDraftRepository extends JpaRepository<CompanyDraft, Long> {

    /** 초안은 인증 1건당 1개(1:1). 조회/자동저장 upsert 의 진입점이다. */
    Optional<CompanyDraft> findByVerificationId(Long verificationId);

    /** 수동 폐기 및 발행 성공 후 정리. 없어도 예외 없이 통과한다(멱등). */
    void deleteByVerificationId(Long verificationId);
}
