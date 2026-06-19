package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.link.CompanyCertification;

public interface CompanyCertificationRepository extends JpaRepository<CompanyCertification, Long> {
}
