package project.plantly.domain.company.policy;

// 등급별 정책 제약 사항을 한 곳에 묶은 값 객체.
// 새 제약이 생기면 이 record 에 필드를 추가하고, GradePolicyRegistry 의 등급별 정의만 채우면 된다.
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
        int maxDetailImages,
        // 브랜드 컬러 커스텀 지정 가능 여부 (false = 기본값으로 고정)
        boolean customBrandColorAllowed,
        // 등록 시점 spotlight 자동 활성화 여부
        boolean spotlightOnCreate
) {
}
