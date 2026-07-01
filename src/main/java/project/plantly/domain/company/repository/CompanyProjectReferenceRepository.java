package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyProjectReference;

import java.util.Optional;

public interface CompanyProjectReferenceRepository extends JpaRepository <CompanyProjectReference, Long> {

    // 상세 조회용. 대표로 지정된 레퍼런스 1건만 가져온다. (전체 목록은 추후 '더보기' 전용 조회로 분리)
    Optional<CompanyProjectReference> findFirstByCompanyIdAndRepresentativeTrueOrderByDisplayOrderAsc(Long companyId);

    // 컬렉션 전체 교체(PUT) 시 기존 레퍼런스를 일괄 삭제한다. (딸린 프로젝트 이미지는 호출부에서 먼저 삭제)
    void deleteByCompanyId(Long companyId);
}
