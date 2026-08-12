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

        // 카드 커버 이미지(없으면 null → 프론트가 자체 자리표시자로 폴백). 로고와 다른 축이다 — 로고는 정사각 배지,
        // 커버는 카드 배경에 깔리는 와이드 사진이라 같은 값을 두 자리에 쓸 수 없다.
        String coverImageUrl,

        // 브랜드 컬러(#RRGGBB, 미지정이면 null). 메인 스팟라이트 배너 배경에 쓴다.
        // 스팟라이트 레일만 필요한 값이지만 전용 프로젝션을 따로 만들지 않는다 — company 행의 스칼라라 추가 비용이
        // 사실상 없는 반면, 레일별로 프로젝션을 쪼개면 5개 읽기 경로가 공유하던 카드 매퍼가 갈라진다.
        // 저장값을 그대로 읽는다(조회 시점 파생 아님, 등급 게이트 없음). 게이트는 이 값을 실제로 쓰는 곳이 갖는다 —
        // 스팟라이트 레일(ShowcaseCardRepository)이 후보를 뽑을 때 이미 자격을 거르므로, 그 결과에 실린 색은 자격을
        // 통과한 값이다. 여기서 다시 판단하면 아무도 안 읽는 4개 경로까지 구독 조인을 지불하게 된다.
        // 새 화면이 이 값을 쓰기 시작하면 게이트는 그 화면의 조회 쪽에 붙인다.
        String brandColor,

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
        return new CompanySummary(id, companyName, introTitle, logoUrl, coverImageUrl, brandColor, address,
                verified, featured, spotlight, categoryNames, tagNames, industryNames,
                likedByMe, favoritedByMe);
    }
}
