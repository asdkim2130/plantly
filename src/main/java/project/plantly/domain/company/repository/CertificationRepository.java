package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.Certification;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
}
