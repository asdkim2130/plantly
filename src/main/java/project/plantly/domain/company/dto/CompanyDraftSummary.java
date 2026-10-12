package project.plantly.domain.company.dto;

import java.time.LocalDateTime;

/**
 * 내 임시저장 초안 목록의 한 줄. "작성 중인 등록 · 이어서 쓰기" 를 그리는 데 필요한 것만 싣는다.
 *
 * <p>payload 전체를 싣지 않는 이유: 목록은 어느 초안을 이어 쓸지 고르는 화면이고, 폼 복원은 고른 뒤에
 * 단건 조회({@code GET /companies/drafts/{businessNumber}})가 이어쓸 인증과 함께 한다. 목록에 폼 전체를
 * 실으면 쓰지도 않는 컬렉션까지 초안 수만큼 내려간다.
 *
 * <p>companyName 은 payload 에서 꺼낸 값이다 — 자동저장은 부분 입력을 허용하므로 아직 안 썼으면 null 이고,
 * 화면은 사업자번호로 대신 부른다.
 */
public record CompanyDraftSummary(
        String businessNumber,
        String companyName,
        LocalDateTime updatedAt
) {
}
