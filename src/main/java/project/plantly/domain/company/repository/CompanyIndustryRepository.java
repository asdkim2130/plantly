package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.plantly.domain.company.entity.link.CompanyIndustry;
import project.plantly.domain.company.industry.Industry;

import java.util.List;

public interface CompanyIndustryRepository extends JpaRepository<CompanyIndustry, Long> {

    // 링크를 거쳐 연결된 산업군 마스터를 한 번의 조회로 가져온다. (링크별 LAZY 로딩 N+1 회피)
    @Query("select l.industry from CompanyIndustry l where l.company.id = :companyId order by l.id")
    List<Industry> findIndustriesByCompanyId(@Param("companyId") Long companyId);
}
