package project.plantly.domain.company.domesticRegion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DomesticRegionRepository extends JpaRepository<DomesticRegion, Long> {

    // 전체를 code 순으로 조회 — 트리 조립 시 시도/시군구가 자연스러운 순서로 정렬된다.
    List<DomesticRegion> findAllByOrderByCodeAsc();

    // 공개 옵션용. code 순 정렬이 곧 관습적 배열 순서라 별도 정렬이 필요 없다
    // (법정동코드가 서울 11 → 부산 26 → … 순이고, 합성 코드 0000000000 인 전국이 맨 앞에 온다).
    List<DomesticRegion> findByActiveTrueOrderByCodeAsc();
}
