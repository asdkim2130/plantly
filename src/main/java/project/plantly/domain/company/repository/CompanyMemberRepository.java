package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.link.CompanyMember;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, Long> {

    // 소유자 전용 상세 뷰 접근 제어용. 요청자가 해당 회사의 멤버인지 확인한다.
    // (현재 멤버는 OWNER 1건뿐이라 사실상 소유자 검증과 동치)
    boolean existsByCompanyIdAndUserId(Long companyId, Long userId);
}
