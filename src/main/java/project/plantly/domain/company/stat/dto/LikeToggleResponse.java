package project.plantly.domain.company.stat.dto;

// 좋아요 토글 결과. liked = 이번 요청 후의 상태(true=좋아요 켜짐), likeCount = 갱신된 회사 총 좋아요 수.
public record LikeToggleResponse(boolean liked, long likeCount) {
}
