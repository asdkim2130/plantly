package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.plantly.domain.company.country.Country;
import project.plantly.domain.company.entity.link.CompanyCountry;

import java.util.List;

public interface CompanyCountryRepository extends JpaRepository<CompanyCountry, Long> {

    // 링크를 거쳐 연결된 국가 마스터를 한 번의 조회로 가져온다. (링크별 LAZY 로딩 N+1 회피)
    @Query("select l.country from CompanyCountry l where l.company.id = :companyId order by l.displayOrder, l.id")
    List<Country> findCountriesByCompanyId(@Param("companyId") Long companyId);
}
