package project.plantly.domain.company.search.dto;

import java.util.List;

/**
 * 회사 검색 결과 1건(리스트 카드). Company 스칼라 + 회사가 연결한 카테고리/태그/산업군 이름 목록.
 * 이름 목록은 검색 시점에 링크 테이블에서 집계한다(카테고리는 회사가 직접 고른 것만 — closure 조상은 제외).
 */
public record CompanySummary(
        Long id,
        String companyName,
        String introTitle,
        String logoUrl,

        // 표시용 지역 라벨: 도로명 주소에서 시도+시군구까지만 남긴 값("서울시 강남구" / "경기 화성시" / "세종특별자치시").
        // 전체 주소(도로명·지번·상세)는 상세 조회 응답에서 본다.
        String address,
        boolean verified,
        boolean featured,
        boolean spotlight,
        List<String> categoryNames,
        List<String> tagNames,
        List<String> industryNames,

        // 개인화(로그인 뷰어 기준): 내가 이 회사를 좋아요/즐겨찾기 했는지. 검색 프로젝션은 viewer 독립이라 false 로 만들고,
        // 조회 서비스가 뷰어별 배치 조회로 채운다(withViewerFlags). 익명 뷰어는 false 유지.
        boolean likedByMe,
        boolean favoritedByMe
) {

    // 검색 결과 카드에 뷰어별 개인화 상태를 덧입힌 사본을 만든다. (record 는 불변이라 교체 생성)
    public CompanySummary withViewerFlags(boolean likedByMe, boolean favoritedByMe) {
        return new CompanySummary(id, companyName, introTitle, logoUrl, address,
                verified, featured, spotlight, categoryNames, tagNames, industryNames,
                likedByMe, favoritedByMe);
    }
}
