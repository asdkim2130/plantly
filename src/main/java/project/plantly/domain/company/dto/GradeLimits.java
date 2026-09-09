package project.plantly.domain.company.dto;

import project.plantly.domain.company.policy.GradePolicy;

/**
 * 등급이 허용하는 입력 한도. 폼이 입력 개수를 제어하는 근거다.
 *
 * <p>서버가 계산해 내려주는 이유는 {@link project.plantly.domain.company.policy.GradePolicyRegistry} 를
 * 한도의 단일 출처로 유지하기 위해서다. 프론트가 등급→한도 표를 따로 들고 있으면 표를 고칠 때마다
 * 두 곳이 어긋나고, 프론트가 통과시킨 값을 서버가 400 으로 막는 상태가 된다.
 *
 * <p>내부 정책 표({@link GradePolicy})를 그대로 노출하지 않고 옮겨 담는다 — 정책 표는 새 제약이 생길 때마다
 * 필드가 늘어나는 내부 구조라, 그대로 내보내면 표를 고칠 때 API 계약이 함께 깨진다.
 *
 * <p>등록 폼(인증 응답)과 수정 폼(구독 조회)이 같은 모양을 쓴다. 두 화면이 같은 것을 묻고 있는데
 * 응답 모양이 다르면 프론트가 한도 해석 코드를 두 벌 들게 된다.
 *
 * <p>videoAllowed 는 개수가 아니라 노출 자격이다. 동영상은 등급과 무관하게 저장되고 공개 조회에서만
 * 가려지므로(입력은 막지 않는다), 폼은 이 값으로 입력란을 감출지 자물쇠 안내를 붙일지 정한다.
 */
public record GradeLimits(
        int maxCategories,
        int maxDetailImages,
        int maxReferenceImages,
        boolean videoAllowed
) {

    public static GradeLimits from(GradePolicy policy) {
        return new GradeLimits(
                policy.maxCompanyCategories(),
                policy.maxDetailImages(),
                policy.maxReferenceImages(),
                policy.videoAllowed());
    }
}
