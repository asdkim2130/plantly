package project.plantly.domain.company.dto;

import project.plantly.domain.company.search.dto.CompanySummary;

import java.util.List;

/**
 * 메인 화면 노출 영역 응답. 스팟라이트·추천·최근 등록 세 레일을 한 번에 내려준다.
 *
 * <p>목록/검색과 달리 페이지가 아니라 '자리 수만큼의 고정 리스트'라 {@code PageResponse} 를 쓰지 않는다.
 * 프론트가 회사 목록을 받아 플래그로 걸러내면 자리보다 후보가 많아지는 순간 조용히 어긋나므로,
 * 어느 회사가 어느 자리에 뜨는지는 서버가 정해서 내려준다.
 *
 * <p>레일끼리 같은 회사가 함께 나올 수 있다(고정 + 추천, 또는 유료 고객이 최근 등록에도). 프론트에서
 * 중복 제거하지 않는다 — 각 영역이 독립적으로 의미를 갖는 노출이라 의도된 동작이다.
 */
public record CompanyShowcaseResponse(
        List<CompanySummary> spotlight,
        List<CompanySummary> featured,

        // 최근 등록 회사. 이름 그대로 '레일'이지 목록이 아니다 — 여기에 페이징이나 패싯을 붙이면 안 된다.
        //
        // 메인의 이 지면이 공개 목록 API 를 쓰지 않는 이유는 정렬이 서로 다른 것을 뜻하기 때문이다.
        // 목록/검색의 기본 정렬(spotlight → featured → 최신)은 "어떤 검색어·패싯에도 유료 고객을 상위로"라는
        // 요금제 계약이고, 그건 위 두 레일이 이미 수행한 노출이다. 같은 정렬을 바로 아래에 다시 적용하면
        // 방금 본 회사가 같은 순서로 재등장한다. 그래서 지면을 나눴다 — 계약은 목록/검색 화면에 그대로 남는다.
        //
        // 프론트 계약: '더보기'는 여기서 더 받아오는 게 아니라 목록 화면(GET /api/v1/companies)으로 넘어간다.
        // 카테고리 패싯을 고른 뒤의 목록도 이 필드가 아니라 그 목록 API 가 담당한다(패싯이 걸리는 순간
        // 유료 상위 노출이 다시 적용돼야 하므로, 두 지면의 정렬이 갈리는 것이 맞다).
        List<CompanySummary> latest
) {
}
