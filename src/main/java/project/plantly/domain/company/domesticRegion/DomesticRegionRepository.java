package project.plantly.domain.company.domesticRegion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DomesticRegionRepository extends JpaRepository<DomesticRegion, Long> {

    // 전체를 code 순으로 조회 — 트리 조립 시 시도/시군구가 자연스러운 순서로 정렬된다.
    List<DomesticRegion> findAllByOrderByCodeAsc();
}
