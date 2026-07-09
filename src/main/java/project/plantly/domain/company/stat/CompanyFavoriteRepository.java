package project.plantly.domain.company.stat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CompanyFavoriteRepository extends JpaRepository<CompanyFavorite, Long> {

    // 토글 판별용. 요청자가 이미 해당 회사를 즐겨찾기 중인지 확인한다.
    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);

    // 즐겨찾기 해제(hard delete). 삭제된 행 수를 반환한다.
    long deleteByUserIdAndCompanyId(Long userId, Long companyId);

    // 내 즐겨찾기 목록(최신순 페이징). 즐겨찾기는 좋아요와 달리 유저가 목록으로 관리한다.
    Page<CompanyFavorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 목록 개인화용 배치 조회. 한 페이지의 회사 id 들 중 이 유저가 즐겨찾기한 것만 id 로 돌려준다(한 쿼리).
    @Query("select f.companyId from CompanyFavorite f where f.userId = :userId and f.companyId in :companyIds")
    List<Long> findFavoritedCompanyIds(@Param("userId") Long userId, @Param("companyIds") Collection<Long> companyIds);
}
