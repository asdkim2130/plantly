package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyEquipment;

public interface CompanyEquipmentRepository extends JpaRepository<CompanyEquipment, Long> {
}
