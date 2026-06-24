package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyMaterial;

import java.util.List;

public interface CompanyMaterialRepository extends JpaRepository<CompanyMaterial, Long> {

    // 상세 조회 조립용. 저장 시 부여한 displayOrder 순서를 그대로 노출한다.
    List<CompanyMaterial> findByCompanyIdOrderByDisplayOrderAsc(Long companyId);
}
