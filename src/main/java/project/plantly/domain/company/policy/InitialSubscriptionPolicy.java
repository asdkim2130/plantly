package project.plantly.domain.company.policy;

import org.springframework.stereotype.Component;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.entity.CompanyVerification;
import project.plantly.domain.company.enums.CompanyGrade;

import java.time.LocalDate;

// 자가등록 회사가 받을 '초기 등급'의 단일 출처.
//
// 이 클래스가 존재하는 이유는 확장성이 아니라 공유다. 초기 등급을 아는 곳이 두 군데여야 한다:
//  - 국세청 인증 발급(CompanyVerificationService): 등록 폼이 몇 개까지 입력 가능한지 미리 알려주려면 필요하다.
//  - 회사 생성(CompanyService): 그 한도로 실제 검증하고 구독을 저장한다.
// 두 곳이 각자 판단하면 "폼은 10개를 받아놓고 서버는 1개에서 거부하는" 어긋남이 생긴다.
//
// 이 분리가 푸는 모순: 등급이 Company 에 붙어 있어 "회사를 만들어야 등급을 안다"고 보이지만, 자가등록의 등급을
// 정하는 재료(CompanyVerification)는 회사보다 먼저 존재한다. 등급은 생성의 결과가 아니라 인증의 결과다.
//
// 확장 지점(정식 요금제와 함께 구현): 같은 사업자번호로 등록했다가 삭제한 뒤 재등록하는 경우는 체험을 주지 않고
// FREE 로 시작한다. 판별은 "삭제 포함 같은 사업자번호의 회사가 이미 있었는가" 한 번의 조회로 끝난다 —
// 활성 회사는 등록 시점에 이미 거부되므로(BUSINESS_NUMBER_TAKEN), 존재한다면 그건 삭제된 이력뿐이다.
// 그 판정은 인증 발급 시점에 내려 CompanyVerification 에 저장하는 편이 낫다. 발급과 생성 사이에 답이 갈리지 않고,
// "이 회사가 왜 체험을 받았는가"가 기록으로 남는다. (effectiveGrade 와 달리 시간이 지나도 답이 바뀌지 않는
//  시점 고정 사실이라 저장해도 안전하다.)
@Component
public class InitialSubscriptionPolicy {

    // 국세청 인증을 직접 통과해 자가등록한 회사에 주는 체험 등급.
    // 최상위 등급이라 등록 시점에는 어떤 컬렉션도 한도에 걸리지 않는다 — 체험 경로가 특수 케이스가 아니라
    // '한도가 최대치인 일반 경로'가 된다.
    private static final CompanyGrade SELF_REGISTRATION_TRIAL_GRADE = CompanyGrade.ENTERPRISE;

    // 등록 폼에 내려줄 초기 등급. 인증 발급 시점에 호출된다(아직 회사가 없다).
    public CompanyGrade initialGrade(CompanyVerification verification) {
        return SELF_REGISTRATION_TRIAL_GRADE;
    }

    // 등록 트랜잭션에서 저장할 초기 구독.
    //
    // expiresAt 을 null(무기한)로 두는 것은 의도다. 체험 기간은 한 달로 정해져 있지만 만료 재조정(초과 컬렉션
    // 비활성화 + 색인 재생성)이 아직 없다. 여기서 만료일을 채우면 effectiveGrade 가 파생값이라 배치 없이도
    // 한 달 뒤 저절로 FREE 로 떨어진다 — 그 순간 수정은 FREE 한도로 거부되고 동영상은 가려지는데, 이미 노출 중인
    // 컬렉션은 재조정할 주체가 없어 그대로 남는다. 만료가 '미구현'이 아니라 '절반만 구현된 채 자동 발동'하는 상태다.
    //
    // status 를 ACTIVE 가 아니라 TRIAL 로 남기는 이유: 요금제가 붙었을 때 체험과 결제를 구분할 수 있어야 한다.
    // 기한을 거는 작업은 한 문장으로 끝난다 — status='TRIAL' AND expires_at IS NULL 인 구독에 만료일을 채우면 된다.
    public CompanySubscription initialSubscription(CompanyVerification verification, LocalDate today) {
        return CompanySubscription.trial(initialGrade(verification), today, null);
    }
}
