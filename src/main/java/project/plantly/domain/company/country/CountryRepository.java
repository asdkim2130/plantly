package project.plantly.domain.company.country;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 국가 마스터.
 *
 * <p>정렬 파생 쿼리(findAllByOrderByNameKoAsc)를 두지 않는다 — 국가명 정렬은 DB collation 에
 * 좌우되어 이식성이 없다. 정렬 근거는 {@code CountryService#getPublicList} 주석 참고.
 */
public interface CountryRepository extends JpaRepository<Country, Long> {
}
