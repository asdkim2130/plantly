package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyMaterial;

public interface CompanyMaterialRepository extends JpaRepository<CompanyMaterial, Long> {
}
