package project.plantly.domain.company.dto;

import java.time.LocalDateTime;

/**
 * 임시저장 초안 조회 응답. 저장했던 폼 상태(payload)와 마지막 저장 시각을 함께 준다.
 * 프런트는 payload 로 폼 전체(기본 필드 + 컬렉션)를 복원하고, updatedAt 으로 "n분 전 저장됨" 등을 표시한다.
 */
public record CompanyDraftResponse(
        MyCompanyCreateRequest payload,
        LocalDateTime updatedAt
) {
}
