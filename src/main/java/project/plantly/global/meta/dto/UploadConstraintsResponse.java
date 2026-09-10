package project.plantly.global.meta.dto;

import project.plantly.domain.upload.ImageFormat;
import project.plantly.domain.upload.storage.StorageProperties;

import java.util.Arrays;
import java.util.List;

/**
 * 이미지 업로드 제약. 프론트가 파일을 고르는 순간 스스로 막을 근거다.
 *
 * <p>계약으로 여는 이유는 두 값의 성격이 다르기 때문이다.
 * <ul>
 *   <li>{@code maxFileSizeBytes} 는 <b>배포마다 달라진다</b> — {@code app.upload.max-file-size} 가
 *       환경변수({@code UPLOAD_MAX_FILE_SIZE})로 주입되므로 코드 상수가 아니다. 프론트가 10MB 를
 *       하드코딩하면 상한을 낮춘 배포에서 조용히 어긋나, 프론트가 통과시킨 파일이 서버에서 400 이 된다.</li>
 *   <li>{@code allowed*} 는 사실상 불변이지만({@link ImageFormat} 이 시그니처 판별 코드와 짝이라 형식을
 *       늘리려면 코드를 짜야 한다) <b>같은 목록이 두 곳에 살면 안 된다</b>. 형식을 하나 늘렸을 때
 *       {@code <input accept>} 가 따라오지 않으면 사용자는 고를 수 없는 형식을 서버만 받아들이는 상태가 된다.</li>
 * </ul>
 *
 * <p>바이트로 내려보내는 이유는 프론트가 비교할 대상이 {@code File.size}(바이트)이기 때문이다.
 * "10MB" 같은 표기는 화면이 만든다 — 사람이 읽는 문구는 서버가 소유하지 않는다
 * ({@link project.plantly.global.meta.OptionCatalog} 의 라벨 기준과 같은 판단).
 *
 * <p>등급별 <b>장수</b> 제한은 여기 없다. 회사마다 다른 값이라 이 정적 응답에 담을 수 없고,
 * 등록 폼은 인증 응답의 {@code limits}, 수정 폼은 구독 조회의 {@code limits} 를 본다. 여기 있는 것은
 * 등급과 무관한 "한 장당" 제약이다.
 */
public record UploadConstraintsResponse(
        long maxFileSizeBytes,
        List<String> allowedContentTypes,
        List<String> allowedExtensions
) {

    /**
     * 설정과 {@link ImageFormat} 에서 도출한다. 목록을 손으로 나열하지 않는 것이 핵심 —
     * 형식을 추가하면 이 응답이 저절로 따라온다.
     */
    public static UploadConstraintsResponse of(StorageProperties storageProperties) {
        return new UploadConstraintsResponse(
                storageProperties.maxFileSize().toBytes(),
                Arrays.stream(ImageFormat.values()).map(ImageFormat::getContentType).toList(),
                Arrays.stream(ImageFormat.values()).map(ImageFormat::getExtension).toList());
    }
}
