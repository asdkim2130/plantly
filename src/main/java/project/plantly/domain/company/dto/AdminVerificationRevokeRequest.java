package project.plantly.domain.company.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 사업자 인증 회수 요청.
 *
 * <p>사유를 필수로 받는다. 인증은 나중에 첫 달 혜택의 자격 조건이 되므로, 되돌린 근거가 남지 않으면
 * 사용자 이의 제기에 답할 수 없다.
 */
public record AdminVerificationRevokeRequest(
        @NotBlank
        String reason
) {
}
