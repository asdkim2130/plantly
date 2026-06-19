package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyImage;

public interface CompanyImageRepository extends JpaRepository<CompanyImage, Long> {
}
