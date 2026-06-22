package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.link.CompanyDomesticRegion;

public interface CompanyDomesticRegionRepository extends JpaRepository<CompanyDomesticRegion, Long> {
}
