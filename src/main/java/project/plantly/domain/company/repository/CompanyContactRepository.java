package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyContact;

public interface CompanyContactRepository extends JpaRepository<CompanyContact, Long> {
}
