package project.plantly.domain.company.industry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IndustryRepository extends JpaRepository<Industry, Long> {

    boolean existsBySlug(String slug);
    boolean existsByIndustryName(String industryName);

    // 전체를 displayOrder 순으로 조회 (관리자 목록 정렬용)
    List<Industry> findAllByOrderByDisplayOrderAsc();

    // 공개 옵션 목록: 폐기(active=false)된 산업은 신규 선택지로 노출하지 않는다.
    // (이미 그 산업을 링크한 회사의 상세에는 계속 보인다 — 노출 대상이 다른 경로다)
    List<Industry> findAllByActiveTrueOrderByDisplayOrderAsc();

    // 전체 산업군 중 최대 displayOrder — 없으면 -1 (자동 순번 부여용, +1 하면 첫 항목은 0)
    @Query("select coalesce(max(i.displayOrder), -1) from Industry i")
    int findMaxDisplayOrder();
}