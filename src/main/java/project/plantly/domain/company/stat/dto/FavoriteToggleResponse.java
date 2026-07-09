package project.plantly.domain.company.stat.dto;

// 즐겨찾기 토글 결과. favorited = 이번 요청 후의 상태(true=즐겨찾기 켜짐). 총 개수는 노출하지 않는다(필요 시 COUNT(*) 파생).
public record FavoriteToggleResponse(boolean favorited) {
}
