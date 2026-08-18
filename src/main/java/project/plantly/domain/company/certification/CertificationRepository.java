package project.plantly.domain.company.certification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CertificationRepository extends JpaRepository<Certification, Long> {

    boolean existsByCertificationName(String certificationName);

    boolean existsBySlug(String slug);

    // 전체를 displayOrder 순으로 조회 (관리자 목록 정렬용)
    List<Certification> findAllByOrderByDisplayOrderAsc();

    // 공개 옵션 목록: 폐기(active=false)된 인증은 신규 선택지로 노출하지 않는다.
    // (이미 그 인증을 링크한 회사의 상세에는 계속 보인다 — 노출 대상이 다른 경로다)
    List<Certification> findAllByActiveTrueOrderByDisplayOrderAsc();

    // 위 공개 목록의 건수. 현황 지표가 선택지 수와 어긋나지 않도록 같은 조건(active)을 쓴다.
    long countByActiveTrue();

    // 전체 인증 중 최대 displayOrder — 없으면 -1 (자동 순번 부여용, +1 하면 첫 항목은 0)
    @Query("select coalesce(max(c.displayOrder), -1) from Certification c")
    int findMaxDisplayOrder();
}
