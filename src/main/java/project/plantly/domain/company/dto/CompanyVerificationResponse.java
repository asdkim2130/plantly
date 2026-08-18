package project.plantly.domain.company.dto;

import project.plantly.domain.company.entity.CompanyVerification;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.policy.GradePolicy;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 인증 발급 결과. verificationId 를 회사 등록 요청에 그대로 실어 보내면 서버가 검증된 값을 채운다.
 *
 * <p>검증된 세 값을 함께 돌려주는 이유는, 등록 폼에서 읽기 전용으로 보여주기 위해서다. 사용자가 다시
 * 입력하게 하면 인증본과 다른 값이 저장될 여지가 생긴다(등록 요청은 이 세 값을 받지 않는다).
 *
 * <p>초기 등급과 한도를 함께 내리는 이유는 순서 때문이다. 등급은 Company 에 붙어 있지만 컬렉션 입력은
 * 회사가 생기기 전에 끝나므로, 폼이 한도를 알 방법이 인증 응답밖에 없다. 서버가 계산해 내려주면
 * GradePolicyRegistry 가 한도의 단일 출처로 유지된다 — 프론트가 등급→한도 표를 따로 들고 있으면
 * 표를 고칠 때마다 두 곳이 어긋난다.
 */
public record CompanyVerificationResponse(
        Long verificationId,
        String businessNumber,
        String ceoName,
        LocalDate businessStartDate,
        LocalDateTime expiresAt,

        // 이 인증으로 회사를 만들면 받게 될 등급. 등록 폼의 안내 문구·배지에 쓴다.
        // (등록 후 실제 구독 상태는 구독 조회 API 가 내려준다 — 이건 '예고'다)
        CompanyGrade initialGrade,
        // 그 등급이 허용하는 입력 한도. 폼이 입력 개수를 제어하는 근거다.
        InitialLimits limits
) {

    public static CompanyVerificationResponse from(CompanyVerification verification,
                                                   CompanyGrade initialGrade, GradePolicy policy) {
        return new CompanyVerificationResponse(
                verification.getId(),
                verification.getBusinessNumber(),
                verification.getCeoName(),
                verification.getBusinessStartDate(),
                verification.getExpiresAt(),
                initialGrade,
                InitialLimits.from(policy));
    }

    /**
     * 등록 폼이 읽는 한도. 내부 정책 표({@link GradePolicy})를 그대로 노출하지 않고 옮겨 담는다 —
     * 정책 표는 새 제약이 생길 때마다 필드가 늘어나는 내부 구조라, 그대로 내보내면 표를 고칠 때
     * API 계약이 함께 깨진다.
     *
     * <p>videoAllowed 는 개수가 아니라 노출 자격이다. 동영상은 등급과 무관하게 저장되고 공개 조회에서만
     * 가려지므로(입력은 막지 않는다), 폼은 이 값으로 입력란을 감출지 자물쇠 안내를 붙일지 정한다.
     */
    public record InitialLimits(
            int maxCategories,
            int maxDetailImages,
            int maxReferenceImages,
            boolean videoAllowed
    ) {
        public static InitialLimits from(GradePolicy policy) {
            return new InitialLimits(
                    policy.maxCompanyCategories(),
                    policy.maxDetailImages(),
                    policy.maxReferenceImages(),
                    policy.videoAllowed());
        }
    }
}
