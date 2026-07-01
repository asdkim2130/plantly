package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyImage;

import java.util.List;

public interface CompanyImageRepository extends JpaRepository<CompanyImage, Long> {

    // 회사의 모든 이미지(갤러리 + 프로젝트)를 한 번에 가져온다.
    // 갤러리(projectReference == null)와 프로젝트 이미지의 분리는 조립 단계에서 수행한다.
    List<CompanyImage> findByCompanyIdOrderByDisplayOrderAsc(Long companyId);

    // 갤러리 이미지(projectReference IS NULL)만 교체 삭제한다. 프로젝트 이미지는 건드리지 않는다.
    void deleteByCompanyIdAndProjectReferenceIsNull(Long companyId);

    // 프로젝트 레퍼런스 이미지(projectReference IS NOT NULL)만 삭제한다. 레퍼런스 삭제 전 FK 정리용.
    void deleteByCompanyIdAndProjectReferenceIsNotNull(Long companyId);
}
