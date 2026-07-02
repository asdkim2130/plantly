package project.plantly.companyTest.policyTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import project.plantly.companyTest.support.CompanyCreateRequestBuilder;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ImageRequest;
import project.plantly.domain.company.enums.ImageType;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.GradePolicyRegistry;
import project.plantly.domain.company.policy.rule.DetailImageLimitPolicy;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// DetailImageLimitPolicy: 갤러리 DETAIL 이미지 장수가 등급 상한을 넘으면 거부. DETAIL 만 센다.
@DisplayName("DetailImageLimitPolicy: 상세 이미지 장수 상한")
class DetailImageLimitPolicyTest {

    private final DetailImageLimitPolicy policy = new DetailImageLimitPolicy(new GradePolicyRegistry());

    private final CompanySubscription free = CompanySubscription.freeForUser(LocalDate.now());   // 상한 3
    private final CompanySubscription admin = CompanySubscription.adminExempt(LocalDate.now());

    @Test
    @DisplayName("FREE 상한(3) 이내면 통과")
    void free_withinLimit_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().detailImages(3).build();

        assertThatCode(() -> policy.apply(null, request, free)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("FREE 상한(3) 초과면 DETAIL_IMAGE_LIMIT_EXCEEDED")
    void free_overLimit_throws() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().detailImages(4).build();

        assertThatThrownBy(() -> policy.apply(null, request, free))
                .extracting("errorCode")
                .isEqualTo(CompanyErrorCode.DETAIL_IMAGE_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("PROJECT 타입은 카운트에서 제외 — DETAIL 만 상한에 걸린다")
    void onlyDetailCounted_passes() {
        // DETAIL 3장(상한 통과) + PROJECT 5장(무시). 전체 8장이지만 DETAIL 만 세므로 통과해야 한다.
        List<ImageRequest> images = List.of(
                new ImageRequest("d1", ImageType.DETAIL),
                new ImageRequest("d2", ImageType.DETAIL),
                new ImageRequest("d3", ImageType.DETAIL),
                new ImageRequest("p1", ImageType.PROJECT),
                new ImageRequest("p2", ImageType.PROJECT),
                new ImageRequest("p3", ImageType.PROJECT),
                new ImageRequest("p4", ImageType.PROJECT),
                new ImageRequest("p5", ImageType.PROJECT)
        );
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().images(images).build();

        assertThatCode(() -> policy.apply(null, request, free)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("images 가 null 이면 통과")
    void nullImages_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().build();

        assertThatCode(() -> policy.apply(null, request, free)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("관리자 등록은 상한을 초과해도 면제되어 통과")
    void admin_exempt_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().detailImages(10).build();

        assertThatCode(() -> policy.apply(null, request, admin)).doesNotThrowAnyException();
    }
}
