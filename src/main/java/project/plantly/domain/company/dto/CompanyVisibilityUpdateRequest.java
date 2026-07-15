package project.plantly.domain.company.dto;

import jakarta.validation.constraints.NotNull;
import project.plantly.domain.company.enums.CompanyVisibility;

// 공개/비공개 전환 요청. 토글이 아니라 목표 상태(PUBLIC/PRIVATE)를 그대로 지정한다(멱등).
public record CompanyVisibilityUpdateRequest(
        @NotNull CompanyVisibility visibility
) {
}
