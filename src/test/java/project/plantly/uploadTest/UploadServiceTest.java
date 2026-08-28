package project.plantly.uploadTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import project.plantly.domain.upload.ImageFormat;
import project.plantly.domain.upload.UploadService;
import project.plantly.domain.upload.dto.UploadResponse;
import project.plantly.domain.upload.exception.UploadErrorCode;
import project.plantly.domain.upload.storage.FileStorage;
import project.plantly.domain.upload.storage.StorageProperties;
import project.plantly.global.exception.BusinessException;
import project.plantly.global.exception.CommonErrorCode;

import java.io.UncheckedIOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("업로드 서비스")
class UploadServiceTest {

    private static final long USER_ID = 7L;
    private static final String KEY = "0123456789abcdef0123456789abcdef.png";

    @Mock
    private FileStorage fileStorage;

    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        // 상한을 1MB 로 낮춰 잡는다 - 10MB 짜리 배열을 만들지 않고도 경계를 검증할 수 있다.
        StorageProperties properties = new StorageProperties(DataSize.ofMegabytes(1), null);
        uploadService = new UploadService(fileStorage, properties);
    }

    @Test
    @DisplayName("업로드에 성공하면 조회 경로가 붙은 URL 과 판별된 형식을 돌려준다")
    void upload_success() {
        byte[] content = ImageBytes.png();
        given(fileStorage.store(any(), eq(ImageFormat.PNG))).willReturn(KEY);

        UploadResponse response = uploadService.upload(USER_ID, file(content, "image/png"));

        assertThat(response.url()).isEqualTo("/api/v1/files/" + KEY);
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.size()).isEqualTo(content.length);
    }

    @Test
    @DisplayName("클라이언트가 Content-Type 을 image/png 로 위조해도 내용이 이미지가 아니면 거절한다")
    void upload_rejectsForgedContentType() {
        MockMultipartFile forged = file(ImageBytes.notAnImage(), "image/png");

        assertThatThrownBy(() -> uploadService.upload(USER_ID, forged))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(UploadErrorCode.UNSUPPORTED_IMAGE_TYPE);
    }

    @Test
    @DisplayName("허용 형식이 아니면 거절한다 - 형식 판별을 통과하지 못한 파일은 저장소에 닿지 않는다")
    void upload_rejectsUnsupportedFormat() {
        MockMultipartFile wav = file(ImageBytes.riffButNotWebp(), "image/webp");

        assertThatThrownBy(() -> uploadService.upload(USER_ID, wav))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(UploadErrorCode.UNSUPPORTED_IMAGE_TYPE);
    }

    @Test
    @DisplayName("파일이 없거나 비어 있으면 400 으로 끊는다")
    void upload_rejectsMissingFile() {
        assertThatThrownBy(() -> uploadService.upload(USER_ID, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(UploadErrorCode.FILE_REQUIRED);

        assertThatThrownBy(() -> uploadService.upload(USER_ID, file(new byte[0], "image/png")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(UploadErrorCode.FILE_REQUIRED);
    }

    @Test
    @DisplayName("상한을 넘는 파일은 형식을 보기 전에 거절한다 - 서블릿 상한과 별개로 서비스가 스스로 지키는 경계다")
    void upload_rejectsTooLargeFile() {
        byte[] tooLarge = new byte[(int) DataSize.ofMegabytes(1).toBytes() + 1];
        System.arraycopy(ImageBytes.png(), 0, tooLarge, 0, 8);

        assertThatThrownBy(() -> uploadService.upload(USER_ID, file(tooLarge, "image/png")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("저장소 IO 실패는 500 으로 번역한다 - 저장소는 상태 코드를 모른다")
    void upload_translatesStorageFailure() {
        willThrow(new UncheckedIOException(new java.io.IOException("disk full")))
                .given(fileStorage).store(any(), any());

        assertThatThrownBy(() -> uploadService.upload(USER_ID, file(ImageBytes.jpeg(), "image/jpeg")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(UploadErrorCode.STORAGE_FAILURE);
    }

    @Test
    @DisplayName("저장된 파일을 key 로 찾으면 내용과 형식을 함께 돌려준다")
    void load_success() {
        given(fileStorage.load(KEY)).willReturn(Optional.of(new ByteArrayResource(ImageBytes.png())));

        UploadService.StoredFile stored = uploadService.load(KEY);

        assertThat(stored.format()).isEqualTo(ImageFormat.PNG);
        assertThat(stored.resource()).isNotNull();
    }

    @Test
    @DisplayName("형태가 틀린 key 는 저장소를 보지도 않고 404 다 - 그런 파일은 존재할 수 없다")
    void load_malformedKeyIsNotFound() {
        assertThatThrownBy(() -> uploadService.load("../../etc/passwd"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(UploadErrorCode.FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("형태는 맞지만 없는 파일도 같은 404 다")
    void load_missingFileIsNotFound() {
        given(fileStorage.load(KEY)).willReturn(Optional.empty());

        assertThatThrownBy(() -> uploadService.load(KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(UploadErrorCode.FILE_NOT_FOUND);
    }

    private MockMultipartFile file(byte[] content, String contentType) {
        return new MockMultipartFile("file", "photo.png", contentType, content);
    }
}
