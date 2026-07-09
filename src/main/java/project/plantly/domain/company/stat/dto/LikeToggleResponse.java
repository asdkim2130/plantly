package project.plantly.domain.company.stat.dto;

// 좋아요 토글 결과. liked = 이번 요청 후의 상태(true=좋아요 켜짐). 총 개수는 노출하지 않는다(필요 시 COUNT(*) 파생).
public record LikeToggleResponse(boolean liked) {
}
