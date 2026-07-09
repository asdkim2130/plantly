package project.plantly.domain.company.stat;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyLikeRepository extends JpaRepository<CompanyLike, Long> {

    // 토글 판별용. 요청자가 이미 해당 회사에 좋아요 중인지 확인한다.
    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);

    // 좋아요 취소(hard delete). 삭제된 행 수를 반환하므로 토글에서 '존재했는지'를 삭제 한 번으로 판별할 수 있다.
    long deleteByUserIdAndCompanyId(Long userId, Long companyId);
}
