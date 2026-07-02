package project.plantly.companyTest.policyTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import project.plantly.companyTest.support.CompanyCreateRequestBuilder;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ImageRequest;
import project.plantly.domain.company.enums.ImageType;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.rule.GalleryImageTypePolicy;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// GalleryImageTypePolicy(구조 검증): 갤러리(images)에는 DETAIL 만 허용. 등급/등록경로와 무관하게 전원 적용(관리자도 면제 아님).
@DisplayName("GalleryImageTypePolicy: 갤러리 이미지 타입 검증")
class GalleryImageTypePolicyTest {

    private final GalleryImageTypePolicy policy = new GalleryImageTypePolicy();

    private final CompanySubscription free = CompanySubscription.freeForUser(LocalDate.now());
    private final CompanySubscription admin = CompanySubscription.adminExempt(LocalDate.now());

    @Test
    @DisplayName("전부 DETAIL 이면 통과")
    void allDetail_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().detailImages(3).build();

        assertThatCode(() -> policy.apply(null, request, free)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PROJECT 타입이 섞이면 GALLERY_IMAGE_TYPE_NOT_ALLOWED")
    void containsProject_throws() {
        List<ImageRequest> images = List.of(
                new ImageRequest("d1", ImageType.DETAIL),
                new ImageRequest("p1", ImageType.PROJECT)
        );
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().images(images).build();

        assertThatThrownBy(() -> policy.apply(null, request, free))
                .extracting("errorCode")
                .isEqualTo(CompanyErrorCode.GALLERY_IMAGE_TYPE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("구조 검증은 관리자 등록도 면제하지 않는다")
    void admin_notExempt_throws() {
        List<ImageRequest> images = List.of(new ImageRequest("p1", ImageType.PROJECT));
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().images(images).build();

        assertThatThrownBy(() -> policy.apply(null, request, admin))
                .extracting("errorCode")
                .isEqualTo(CompanyErrorCode.GALLERY_IMAGE_TYPE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("images 가 null 이면 통과")
    void nullImages_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().build();

        assertThatCode(() -> policy.apply(null, request, free)).doesNotThrowAnyException();
    }
}
