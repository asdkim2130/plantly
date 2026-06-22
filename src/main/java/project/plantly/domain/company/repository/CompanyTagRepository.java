package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyTag;

public interface CompanyTagRepository extends JpaRepository<CompanyTag, Long> {
}
