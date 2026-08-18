package project.plantly.domain.company.dto;

/**
 * 메인 화면 현황 지표. 플랫폼 규모를 나타내는 네 숫자를 한 번에 내려준다.
 *
 * <p>이 엔드포인트가 따로 있는 이유는, 프론트가 이 숫자들을 <b>다른 목적의 API 를 빌려서</b> 세고
 * 있었기 때문이다 — 회사 수는 목록 API 를 {@code size=1} 로 불러 {@code pageInfo.totalElement} 만
 * 빼 썼고(카드 프로젝션 1건과 count 쿼리를 만들어 카드는 버렸다), 인증 수는 선택지 목록 전체를 받아
 * 길이만 셌다. 개수를 알기 위해 목록을 만드는 구조라, 데이터가 늘수록 화면과 무관한 비용이 커진다.
 *
 * <p>각 숫자는 대응하는 공개 목록과 <b>같은 기준</b>으로 센다. 현황이 "업종 24"인데 드롭다운에 20개만
 * 있으면 둘 중 하나는 거짓말이 되므로, 조건을 새로 적지 않고 공개 경로가 쓰는 조건을 그대로 쓴다
 * (활성 항목만, 카테고리는 비활성 조상 아래 서브트리 제외).
 */
public record CompanyStatsResponse(

        // 공개 노출 중인 회사 수. 비공개·삭제 회사는 빠진다.
        long companyCount,

        // 공개 카테고리 트리의 전체 노드 수(대+중+소). 대분류 개수가 아니다.
        long categoryCount,

        // 공개 옵션으로 제공되는 업종 수.
        long industryCount,

        // 공개 옵션으로 제공되는 인증 항목 수.
        long certificationCount
) {
}
