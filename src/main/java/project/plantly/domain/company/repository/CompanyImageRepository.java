package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyImage;

import java.util.List;

public interface CompanyImageRepository extends JpaRepository<CompanyImage, Long> {

    // 회사의 모든 이미지(갤러리 + 프로젝트)를 한 번에 가져온다.
    // 갤러리(projectReference == null)와 프로젝트 이미지의 분리는 조립 단계에서 수행한다.
    List<CompanyImage> findByCompanyIdOrderByDisplayOrderAsc(Long companyId);
}
