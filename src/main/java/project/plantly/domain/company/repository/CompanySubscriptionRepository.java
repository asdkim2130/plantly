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

    // 불변식 전제 조회. 회사 생성은 CompanyService.persist 한 곳으로 모이고 거기서 회사와 구독을 같은
    // 트랜잭션에 넣으므로, 회사가 있으면 구독도 있다. (DB 는 이걸 강제하지 않는다 — companyId 는 raw 참조라
    // FK 가 없고, 유니크 제약은 중복만 막지 존재를 보장하지 않는다. 보증의 출처는 저 단일 쓰기 경로뿐이다.)
    //
    // 그래서 없다면 조회 실패가 아니라 데이터 정합성 오류다. 404 로 삼키면 회사는 존재하는데 없다고 답하는
    // 셈이라 진단도 복구도 막힌다 — 구독을 새로 만드는 API 가 없어 관리자 경로까지 같이 닫힌다.
    // 읽기/쓰기 양쪽이 이 메서드 하나를 공유해 같은 문장으로 드러낸다.
    default CompanySubscription getByCompanyId(Long companyId) {
        return findByCompanyId(companyId)
                .orElseThrow(() -> new IllegalStateException("회사 구독이 존재하지 않습니다(불변식 위반): companyId=" + companyId));
    }

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
