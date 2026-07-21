package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyVerification;

import java.util.Optional;

public interface CompanyVerificationRepository extends JpaRepository<CompanyVerification, Long> {

    /** 회사 등록 시 인증을 소비하는 경로. userId 를 함께 걸어 타인의 인증을 쓰지 못하게 한다. */
    Optional<CompanyVerification> findByIdAndUserId(Long id, Long userId);

    /** 관리자 인증 회수 / 상태 조회. 등록에 소비된 인증은 companyId 로 찾는다. */
    Optional<CompanyVerification> findByCompanyId(Long companyId);
}
