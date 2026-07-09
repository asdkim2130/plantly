package project.plantly.domain.company.stat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CompanyLikeRepository extends JpaRepository<CompanyLike, Long> {

    // 토글 판별용. 요청자가 이미 해당 회사에 좋아요 중인지 확인한다.
    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);

    // 좋아요 취소(hard delete). 삭제된 행 수를 반환하므로 토글에서 '존재했는지'를 삭제 한 번으로 판별할 수 있다.
    long deleteByUserIdAndCompanyId(Long userId, Long companyId);

    // 목록 개인화용 배치 조회. 한 페이지의 회사 id 들 중 이 유저가 좋아요한 것만 id 로 돌려준다(행마다 N+1 없이 한 쿼리).
    @Query("select l.companyId from CompanyLike l where l.userId = :userId and l.companyId in :companyIds")
    List<Long> findLikedCompanyIds(@Param("userId") Long userId, @Param("companyIds") Collection<Long> companyIds);
}
