package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.plantly.domain.company.dto.OwnerSubscriptionRow;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.enums.MemberRole;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompanySubscriptionRepository extends JpaRepository<CompanySubscription, Long> {

    // 회사의 구독 조회(1:1). 정책 적용·상세 조회에서 현재 등급을 해석할 때 사용한다.
    Optional<CompanySubscription> findByCompanyId(Long companyId);

    // 여러 유저가 '소유(OWNER)'한 회사들의 구독을 (userId, 구독) 짝으로 배치 조회한다. 관리자 유저 목록 등급 배지용.
    // CompanyMember(role=OWNER) 와 CompanySubscription 을 companyId 로 세타 조인한다(둘 다 raw id 참조라 매핑 연관 없음).
    // 소유 회사가 없는 유저는 결과에 안 나온다 → 호출부에서 배지 null 로 처리된다.
    // (userId, 구독 엔티티) multiselect → default 메서드가 OwnerSubscriptionRow 로 감싼다. 구독 엔티티를 그대로 실어
    //  effectiveGrade 파생을 서비스가 엔티티 규칙으로 재사용하게 한다.
    @Query("select cm.userId, s from CompanyMember cm, CompanySubscription s " +
            "where s.companyId = cm.companyId and cm.role = :role and cm.userId in :userIds")
    List<Object[]> findOwnerSubscriptionTuples(@Param("role") MemberRole role,
                                               @Param("userIds") Collection<Long> userIds);

    default List<OwnerSubscriptionRow> findOwnerSubscriptionRows(MemberRole role, Collection<Long> userIds) {
        return findOwnerSubscriptionTuples(role, userIds).stream()
                .map(tuple -> new OwnerSubscriptionRow((Long) tuple[0], (CompanySubscription) tuple[1]))
                .toList();
    }
}
