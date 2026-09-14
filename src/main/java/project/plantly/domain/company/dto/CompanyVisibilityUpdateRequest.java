package project.plantly.domain.company.dto;

import jakarta.validation.constraints.NotNull;
import project.plantly.domain.company.enums.CompanyVisibility;

// 공개/비공개 전환 요청. 토글이 아니라 목표 상태(PUBLIC/PRIVATE)를 그대로 지정한다(멱등).
// 메시지를 적어두는 이유는 다른 폼과 같다 — 생략하면 기본 문구("널이어서는 안됩니다")가 나간다.
// (선택지 문구 "공개/비공개" 는 서버가 주지 않는다 — 라벨 소유 기준은 OptionCatalog 주석 참고)
public record CompanyVisibilityUpdateRequest(
        @NotNull(message = "공개 범위는 필수입니다.") CompanyVisibility visibility
) {
}
