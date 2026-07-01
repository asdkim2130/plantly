package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyTag;

import java.util.List;

public interface CompanyTagRepository extends JpaRepository<CompanyTag, Long> {

    // 상세 조회 조립용. 저장 시 부여한 displayOrder 순서를 그대로 노출한다.
    List<CompanyTag> findByCompanyIdOrderByDisplayOrderAsc(Long companyId);

    // 컬렉션 전체 교체(PUT) 시 기존 태그를 일괄 삭제한다.
    void deleteByCompanyId(Long companyId);
}
