package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    // 좋아요/즐겨찾기 등 상호작용 대상 검증용. 미삭제(공개 가능) 회사만 true. 소프트 삭제는 미존재로 취급한다.
    boolean existsByIdAndDeletedFalse(Long id);
}
