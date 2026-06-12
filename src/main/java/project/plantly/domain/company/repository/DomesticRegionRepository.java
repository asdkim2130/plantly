package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.DomesticRegion;

public interface DomesticRegionRepository extends JpaRepository<DomesticRegion, Long> {
}
