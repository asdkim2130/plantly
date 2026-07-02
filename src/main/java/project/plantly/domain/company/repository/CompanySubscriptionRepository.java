package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanySubscription;

import java.util.Optional;

public interface CompanySubscriptionRepository extends JpaRepository<CompanySubscription, Long> {

    // 회사의 구독 조회(1:1). 정책 적용·상세 조회에서 현재 등급을 해석할 때 사용한다.
    Optional<CompanySubscription> findByCompanyId(Long companyId);
}
