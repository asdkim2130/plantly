package project.plantly.domain.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import project.plantly.domain.upload.dto.UploadResponse;
import project.plantly.global.response.ApiResponse;
import project.plantly.global.security.UserPrincipal;

@RestController
@RequiredArgsConstructor
public class UploadController {

    // 파일명이 UUID 라 같은 URL 의 내용이 바뀌는 일이 없다(교체 = 새 업로드 = 새 URL). 그래서 1년 + immutable 로
    // 못 박아 재검증 요청조차 나가지 않게 한다.
    private static final String IMMUTABLE_CACHE = "public, max-age=31536000, immutable";

    private final UploadService uploadService;

    /**
     * 이미지 업로드. 로그인 필요(SecurityConfig 의 {@code anyRequest().authenticated()} 기본 규칙).
     *
     * <p>회사 등록/수정 화면에서 파일을 고르는 즉시 호출하고, 응답의 {@code url} 을 폼 상태에 담는다.
     * 그 문자열이 그대로 {@code logoUrl}·{@code coverImageUrl}·{@code images[].imageUrl} 로 흘러가므로
     * <b>회사 요청 DTO 는 이 기능 때문에 바뀌지 않는다.</b> 초안 자동저장에도 URL 만 실린다.
     *
     * <p><b>여러 장을 한 번에 받지 않는다.</b> 갤러리는 여러 장을 고르지만, 다건으로 묶으면 한 장이
     * 실패할 때 전부 날아가고 진행률도 뭉개진다. 클라이언트가 장당 한 번씩 부르면 장당 재시도와
     * 장당 진행률이 따로 만들지 않아도 생긴다.
     *
     * <p>{@code required = false} 인 이유는 파트가 없을 때 스프링이 던지는 예외가 전역 fallback 에
     * 걸려 500 으로 나가기 때문이다. null 로 받아 서비스가 400(FILE_REQUIRED)으로 끊는다.
     */
    @PostMapping(value = "/api/v1/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UploadResponse> upload(@AuthenticationPrincipal UserPrincipal principal,
                                              @RequestPart(name = "file", required = false) MultipartFile file) {

        UploadResponse response = uploadService.upload(principal.getUser().getId(), file);
        return ApiResponse.success("파일이 업로드되었습니다.", response);
    }

    /**
     * 업로드된 이미지 서빙. <b>비로그인 허용</b>(SecurityConfig 에서 permitAll) — 이 이미지는 공개 회사
     * 상세에 실려 나가므로 인증을 걸면 브라우저의 {@code <img>} 가 읽지 못한다.
     *
     * <p>응답 봉투({@code ApiResponse})를 쓰지 않는 유일한 엔드포인트다. 바이너리를 그대로 흘려보내야
     * {@code <img src>} 가 성립한다.
     *
     * <p>저장소가 외부(S3/Supabase)로 바뀌면 이 메서드가 파일 대신 서명 URL 로 302 리다이렉트하게 된다.
     * <b>경로와 발급된 URL 은 그대로다.</b>
     */
    @GetMapping("/api/v1/files/{key}")
    public ResponseEntity<Resource> serve(@PathVariable String key) {

        UploadService.StoredFile stored = uploadService.load(key);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stored.format().getContentType()))
                .header(HttpHeaders.CACHE_CONTROL, IMMUTABLE_CACHE)
                .body(stored.resource());
    }
}
