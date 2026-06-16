package project.plantly.global.response;

// 생성(create) API 공통 응답 — 생성된 리소스 식별자를 data 로 감싸 반환한다.
public record IdResponse(Long id) {
}
