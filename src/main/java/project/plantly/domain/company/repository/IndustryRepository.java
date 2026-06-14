package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.Industry;

public interface IndustryRepository extends JpaRepository<Industry, Long> {
}
