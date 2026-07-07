package project.plantly.domain.company.dto;

import project.plantly.domain.company.entity.CompanySubscription;

// (소유자 userId, 그 유저가 OWNER 인 회사의 구독) 짝. userId 로 여러 유저의 구독을 한 번에 배치 조회할 때 쓰는 중간 행.
// effectiveGrade 파생을 SQL 로 옮기지 않고 엔티티 규칙을 재사용하기 위해, 파생 전 구독 엔티티를 그대로 실어 나른다.
public record OwnerSubscriptionRow(
        Long userId,
        CompanySubscription subscription
) {
}
