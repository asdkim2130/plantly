package project.plantly.domain.company.industry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IndustryRepository extends JpaRepository<Industry, Long> {

    boolean existsByIndustryCode(String industryCode);
    boolean existsByIndustryName(String industryName);

    // 전체 산업군 중 최대 displayOrder — 없으면 -1 (자동 순번 부여용, +1 하면 첫 항목은 0)
    @Query("select coalesce(max(i.displayOrder), -1) from Industry i")
    int findMaxDisplayOrder();
}