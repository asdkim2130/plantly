package project.plantly.domain.company.enums;

// 회사 공개 범위. 공개 목록/검색 노출 여부를 가른다.
// PUBLIC  - 공개: 일반 목록/검색·즐겨찾기 목록·공개 상세에 노출된다.
// PRIVATE - 비공개: 공개 목록/검색·즐겨찾기·공개 상세에서 숨겨진다. 소유자 본인 목록과 관리자 목록에서는 계속 보인다.
public enum CompanyVisibility {
    PUBLIC,
    PRIVATE
}
