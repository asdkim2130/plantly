package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.link.CompanyCountry;

public interface CompanyCountryRepository extends JpaRepository<CompanyCountry, Long> {
}
