package project.plantly.domain.company.stat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyFavoriteRepository extends JpaRepository<CompanyFavorite, Long> {

    // 토글 판별용. 요청자가 이미 해당 회사를 즐겨찾기 중인지 확인한다.
    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);

    // 즐겨찾기 해제(hard delete). 삭제된 행 수를 반환한다.
    long deleteByUserIdAndCompanyId(Long userId, Long companyId);

    // 내 즐겨찾기 목록(최신순 페이징). 즐겨찾기는 좋아요와 달리 유저가 목록으로 관리한다.
    Page<CompanyFavorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
