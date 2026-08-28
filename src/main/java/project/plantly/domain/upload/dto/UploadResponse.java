package project.plantly.domain.upload.dto;

/**
 * 업로드 결과.
 *
 * <p>화면이 실제로 쓰는 값은 {@code url} 하나다 — 회사 등록/수정 요청의 {@code logoUrl},
 * {@code coverImageUrl}, {@code images[].imageUrl} 자리에 이 문자열을 그대로 담는다.
 * 나머지 둘은 미리보기 옆에 용량을 표시하거나 업로드 결과를 확인하는 용도의 덤이다.
 *
 * <p>{@code url} 은 <b>상대경로</b>다. 프론트의 {@code /api/*} rewrite 를 그대로 타서 오리진이
 * 갈리지 않고, 나중에 저장소가 외부(S3/Supabase)로 바뀌어도 이 경로가 서명 URL 로 리다이렉트하면
 * 되므로 <b>필드의 의미와 화면 코드가 바뀌지 않는다.</b>
 */
public record UploadResponse(
        String url,
        String contentType,
        long size
) {
}
