package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyContact;

import java.util.Optional;

public interface CompanyContactRepository extends JpaRepository<CompanyContact, Long> {

    // 상세 조회용. 대표로 지정된 연락처 1건만 가져온다. (전체 목록은 추후 '더보기' 전용 조회로 분리)
    Optional<CompanyContact> findFirstByCompanyIdAndRepresentativeTrueOrderByDisplayOrderAsc(Long companyId);

    // 컬렉션 전체 교체(PUT) 시 기존 연락처를 일괄 삭제한다.
    void deleteByCompanyId(Long companyId);
}
