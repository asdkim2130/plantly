package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.Country;

public interface CountryRepository extends JpaRepository<Country, Long> {
}
