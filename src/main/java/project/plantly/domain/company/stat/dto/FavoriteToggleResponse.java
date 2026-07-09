package project.plantly.domain.company.stat.dto;

// 즐겨찾기 토글 결과. favorited = 이번 요청 후의 상태(true=즐겨찾기 켜짐), favoriteCount = 갱신된 회사 총 즐겨찾기 수.
public record FavoriteToggleResponse(boolean favorited, long favoriteCount) {
}
