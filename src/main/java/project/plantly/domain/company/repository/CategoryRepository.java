package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
