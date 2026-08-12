package project.plantly.domain.company.policy;

// 등급별 정책 제약 사항을 한 곳에 묶은 값 객체.
// 새 제약이 생기면 이 record 에 필드를 추가하고, GradePolicyRegistry 의 등급별 정의만 채우면 된다.
//
// 여기 담는 것은 '등록/수정 시점에 회사에 반영되는' 제약뿐이다. 스팟라이트 노출처럼 등급에서 파생되지만
// 회사에 저장하지 않고 조회 시점에 구독을 보고 판단하는 혜택은 이 record 에 두지 않는다
// (ShowcaseCardRepository 참고 — 저장하면 구독 만료 시 자동으로 회수되지 않는다).
//
// brandColor 가 이 기준으로 여기서 빠졌다: 쓰기 시점에 등급을 구우면 업그레이드해도 색이 되살아나지 않고
// (사용자가 재입력해야 한다) 다운그레이드는 원래 색을 파괴한다 — 무손실을 전제한 재조정 방침과 충돌한다.
// 지금 brandColor 를 읽는 곳은 스팟라이트 레일 하나뿐이고 그 쿼리가 이미 자격을 걸러내므로,
// 게이트는 값이 저장되는 곳도 프로젝션되는 곳도 아닌 '소비되는 곳'이 갖는다.
// enum(CompanyGrade) 은 "어떤 등급이 존재하는가"(정체성) 만 책임지고,
// "각 등급이 무엇을 허용하는가"(정책) 는 이 record 가 책임진다.
public record GradePolicy(
        // 회사 카테고리(CompanyCategory) 최대 저장 개수
        int maxCompanyCategories,
        // 동영상(videoUrl) 사용 가능 여부
        boolean videoAllowed,
        // 레퍼런스 1건당 이미지 최대 장수 (0 = 업로드 비활성)
        int maxReferenceImages,
        // 회사 직속 갤러리(상세 이미지, ImageType.DETAIL) 최대 장수
        int maxDetailImages
) {
}
