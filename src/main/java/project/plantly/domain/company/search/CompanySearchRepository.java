package project.plantly.domain.company.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import project.plantly.domain.company.search.dto.CompanySummary;

/**
 * 회사 검색 포트(seam). 검색 엔진 교체 지점이다.
 *
 * <p>지금은 {@code PostgresTrigramCompanySearch}(pg_trgm 비정규화 도큐먼트 + 네이티브 SQL, P3)가
 * 구현하고, 데이터·트래픽이 커지면 {@code ElasticCompanySearch} 로 교체한다.
 * {@link CompanySearchCriteria}(통합키워드/고급검색/패싯)와 {@link CompanySummary} 계약은 PG·ES
 * 공통이라, 교체 시 이 인터페이스 뒤 구현만 바뀐다.
 */
public interface CompanySearchRepository {

    Page<CompanySummary> search(CompanySearchCriteria criteria, Pageable pageable);
}
