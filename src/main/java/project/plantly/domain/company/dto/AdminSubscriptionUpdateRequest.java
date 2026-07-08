package project.plantly.domain.company.dto;

import jakarta.validation.constraints.NotNull;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.enums.SubscriptionStatus;

import java.time.LocalDate;

// 관리자 구독 수정 요청. 수정 팝업이 세 필드를 항상 채워 보내는 full-replace 다(부분 수정 아님).
// grade/status 는 필수(드롭다운 선택), expiresAt 은 null = 무기한(만료 없음)을 명시적으로 표현한다.
// startedAt 은 "언제 시작됐나"라는 팩트라 수정 대상에서 제외한다(기존값 보존).
public record AdminSubscriptionUpdateRequest(
        @NotNull(message = "등급은 필수입니다.")
        CompanyGrade grade,
        @NotNull(message = "구독 상태는 필수입니다.")
        SubscriptionStatus status,
        LocalDate expiresAt
) {
}
