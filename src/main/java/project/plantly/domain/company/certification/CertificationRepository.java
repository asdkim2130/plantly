package project.plantly.domain.company.certification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CertificationRepository extends JpaRepository<Certification, Long> {

    boolean existsByCertificationName(String certificationName);

    // 전체 인증 중 최대 displayOrder — 없으면 -1 (자동 순번 부여용, +1 하면 첫 항목은 0)
    @Query("select coalesce(max(c.displayOrder), -1) from Certification c")
    int findMaxDisplayOrder();
}
