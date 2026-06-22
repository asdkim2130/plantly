package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.link.CompanyCategory;

public interface CompanyCategoryRepository extends JpaRepository<CompanyCategory, Long> {
}
