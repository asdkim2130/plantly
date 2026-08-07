package project.plantly.domain.company.dto;

// 관리자 운영 플래그(인증/추천/스팟라이트) 조정 요청. 각 필드는 sparse — null = 미변경, 값 = 목표 상태로 설정(멱등).
// 셋 다 등록 시 false 로 시작하며 관리자만 전환할 수 있다. (검색 도큐먼트엔 없어 재색인 불필요 — 정렬은 company 원본을 실시간 참조)
public record AdminCompanyFlagsRequest(
        Boolean verified,   // 관리자 인증 노출
        Boolean featured,   // 추천 노출
        // 스팟라이트 '수동 고정(pin)'. 메인 노출을 켜는 스위치가 아니다 — 유료 고객은 이 값이 false 여도
        // 구독만으로 노출된다(ShowcaseCardRepository). 요금제 밖에서 띄워야 하는 제휴·이벤트용 통로이며,
        // 켜 두면 자동으로 꺼지지 않는다. 유료 계약은 pin 이 아니라 구독 설정으로 표현해야 나중에
        // "돈 낸 고객"과 "무료로 띄워준 곳"을 데이터로 구별할 수 있다. (순서 spotlightOrder 는 별도)
        Boolean spotlight
) {
}
