package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
