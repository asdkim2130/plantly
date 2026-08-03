package project.plantly.global.seed;

/**
 * 인증·초안 케이스 1건. 회사와 달리 초안은 <b>회사가 되기 전</b>의 상태라, 자연키가 회사 id 가 아니라
 * 인증 id({@code verificationId}) 다 — 초안 조회·저장·폐기 API 가 전부 이 값을 경로 변수로 받는다.
 *
 * @param code           케이스 코드(D01…)
 * @param verificationId 초안 API 의 경로 변수로 쓰는 값
 * @param userCode       소유 계정 코드
 * @param proves         이 행이 무엇을 검증하기 위해 존재하는지
 */
public record SeedDraftRef(String code, Long verificationId, String userCode, String proves) {
}
