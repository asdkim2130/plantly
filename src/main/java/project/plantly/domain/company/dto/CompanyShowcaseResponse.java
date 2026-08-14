package project.plantly.domain.company.dto;

import project.plantly.domain.company.search.dto.CompanySummary;

import java.util.List;

/**
 * 메인 화면 노출 영역 응답. 스팟라이트·추천 두 레일을 한 번에 내려준다.
 *
 * <p>목록/검색과 달리 페이지가 아니라 '자리 수만큼의 고정 리스트'라 {@code PageResponse} 를 쓰지 않는다.
 * 프론트가 회사 목록을 받아 플래그로 걸러내면 자리보다 후보가 많아지는 순간 조용히 어긋나므로,
 * 어느 회사가 어느 자리에 뜨는지는 서버가 정해서 내려준다.
 *
 * <p>두 레일에 같은 회사가 함께 나올 수 있다(고정 + 추천). 프론트에서 중복 제거하지 않는다 —
 * 각 영역이 독립적으로 의미를 갖는 노출이라 의도된 동작이다.
 */
public record CompanyShowcaseResponse(
        List<CompanySummary> spotlight,
        List<CompanySummary> featured
) {
}
