package project.plantly.domain.company.policy;

import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanySubscription;

// 회사 등록 시 적용되는 정책 한 가지를 표현한다.
// 정책 내용(어떤 제약을 어떻게 적용하는지)은 구현체가 전적으로 소유하며,
// 서비스는 주입받은 정책들을 실행만 할 뿐 그 내용을 알지 못한다.
// 새 정책이 필요하면 이 인터페이스를 구현한 @Component 를 추가하기만 하면 된다.
public interface CompanyRegistrationPolicy {

    // 정책 적용. 두 가지를 할 수 있다:
    //  - 검증: 제약 위반 시 BusinessException 을 던진다. (예: 카테고리 상한, 동영상/레퍼런스 이미지 게이팅)
    //  - 변형: 아직 저장되지 않은 company 엔티티를 도메인 메서드로 수정한다. (예: brandColor 고정, spotlight 활성화)
    // 등급은 회사의 구독(subscription)에서 읽는다: 한도는 subscription.effectiveGrade(), 면제는 subscription.isExempt().
    // company 는 save() 이전 상태이므로, 정책이 throw 하면 아무것도 영속화되지 않는다.
    void apply(Company company, CompanyCreateRequest request, CompanySubscription subscription);
}
