package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyProjectReference;

import java.util.Optional;

public interface CompanyProjectReferenceRepository extends JpaRepository <CompanyProjectReference, Long> {

    // 상세 조회용. 대표로 지정된 레퍼런스 1건만 가져온다. (전체 목록은 추후 '더보기' 전용 조회로 분리)
    Optional<CompanyProjectReference> findFirstByCompanyIdAndRepresentativeTrueOrderByDisplayOrderAsc(Long companyId);
}
