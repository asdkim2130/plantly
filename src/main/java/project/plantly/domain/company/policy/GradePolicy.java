package project.plantly.domain.company.policy;

// 등급별 정책 제약 사항을 한 곳에 묶은 값 객체.
// 새 제약이 생기면 이 record 에 필드를 추가하고, GradePolicyRegistry 의 등급별 정의만 채우면 된다.
//
// 이 표는 "등급이 무엇을 허용하는가" 만 소유하고, 그걸 '언제' 강제하는지는 필드마다 다르다:
//  - 개수 한도(maxXxx): 쓰기 시점에 막는다. 저장 비용이 실재하고, 초과분 중 무엇을 보여줄지 고르는 문제가 따라온다.
//  - 불리언 혜택(videoAllowed): 저장은 열어두고 조회 시점에 노출만 가린다.
//
// 불리언을 쓰기에서 강제하면 안 되는 이유: 요금제가 Company 에 붙어 있어 등록 시점엔 모든 회사가 FREE 다.
// 쓰기에서 막으면 등록 폼에서 입력 자체가 불가능하고, 업그레이드 후 같은 값을 다시 넣어야 한다.
// 반대로 다운그레이드는 이미 저장된 값을 파괴한다 — 무손실을 전제한 재조정 방침과 충돌한다.
// CompanySubscription 이 effectiveGrade 를 저장하지 않고 파생하는 것과 같은 이유다(구우면 자동 회수가 안 된다).
//
// brandColor 는 아예 이 표에서 빠졌다. 값을 읽는 곳이 스팟라이트 레일 하나뿐이고 그 쿼리(ShowcaseCardRepository)가
// 이미 자격을 걸러내므로, 표에 남겨두면 아무도 강제하지 않는 항목이 된다. videoAllowed 가 남은 건 상세 조회에는
// 그런 자격 필터가 없어 "어느 등급부터 보여줄지" 를 여전히 누군가 알아야 하기 때문이다(CompanyQueryService 가 읽는다).
//
// enum(CompanyGrade) 은 "어떤 등급이 존재하는가"(정체성) 만 책임지고,
// "각 등급이 무엇을 허용하는가"(정책) 는 이 record 가 책임진다.
public record GradePolicy(
        // 회사 카테고리(CompanyCategory) 최대 저장 개수
        int maxCompanyCategories,
        // 동영상(videoUrl) 공개 노출 가능 여부. 저장은 등급 무관하게 허용되고 조회에서 가려진다.
        boolean videoAllowed,
        // 레퍼런스 1건당 이미지 최대 장수 (0 = 업로드 비활성)
        int maxReferenceImages,
        // 회사 직속 갤러리(상세 이미지, ImageType.DETAIL) 최대 장수
        int maxDetailImages
) {
}
