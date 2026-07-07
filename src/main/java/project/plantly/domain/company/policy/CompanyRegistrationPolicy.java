package project.plantly.domain.company.policy;

// 회사 등급 정책 한 가지를 표현한다. 등록(create)과 수정(update)이 공유하는 정책 계약의 기반.
// 정책 내용(어떤 제약을 어떻게 적용하는지)은 구현체가 전적으로 소유하며, 서비스는 주입받은 정책들을 실행만 한다.
//
// 입력은 CompanyPolicyView(정책이 읽는 것만 추린 통합 뷰)다. 뷰의 슬라이스가 이번 연산의 대상이 아니면(null)
// 각 정책이 스스로 스킵한다 → 등록은 전체를, 수정은 바뀐 부분만 검증한다(같은 코드, 다른 뷰).
//
// apply 는 두 가지를 할 수 있다:
//  - 검증: 제약 위반 시 BusinessException 을 던진다. (예: 카테고리 상한, 동영상/레퍼런스 이미지 게이팅)
//  - 변형: company 엔티티를 도메인 메서드로 수정한다. (예: brandColor 고정, spotlight 활성화)
// 등급은 뷰의 구독(subscription)에서 읽는다: 한도는 effectiveGrade(), 면제는 isExempt().
//
// 등록 전용(create) 정책은 이 인터페이스만 구현하고, 수정에도 재실행돼야 하는 정책은 CompanyMutationPolicy 를 구현한다.
public interface CompanyRegistrationPolicy {

    void apply(CompanyPolicyView view);
}
