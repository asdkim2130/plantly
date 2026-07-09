package project.plantly.domain.company.stat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyStatRepository extends JpaRepository<CompanyStat, Long> {

    // 회사당 1행. 집계 갱신/노출 시 회사 식별자로 조회한다.
    Optional<CompanyStat> findByCompanyId(Long companyId);
}
