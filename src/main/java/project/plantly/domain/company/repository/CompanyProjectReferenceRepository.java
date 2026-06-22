package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyProjectReference;

public interface CompanyProjectReferenceRepository extends JpaRepository <CompanyProjectReference, Long> {
}
