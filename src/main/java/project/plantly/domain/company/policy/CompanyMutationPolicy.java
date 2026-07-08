package project.plantly.domain.company.policy;

// 등록뿐 아니라 '수정(update)'에도 재실행돼야 하는 등급 정책을 표시하는 마커.
// (수정으로 등급 한도를 우회하는 구멍을 닫기 위함 — 등급 한도·게이팅·brandColor 고정이 여기 해당한다.)
//
// - create: CompanyService 가 List<CompanyRegistrationPolicy> 로 전 정책을 실행한다(이 마커 포함).
// - update: CompanyUpdateService 가 List<CompanyMutationPolicy> 로 이 마커를 단 정책만 실행한다.
// 등록 시점 개념인 정책(예: SpotlightPolicy)이나 writer 가 이미 강제하는 구조 검증(GalleryImageTypePolicy)은
// 이 마커를 달지 않아 수정 경로에서 자연히 제외된다.
public interface CompanyMutationPolicy extends CompanyRegistrationPolicy {
}
