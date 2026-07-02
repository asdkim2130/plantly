package project.plantly.companyTest.policyTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import project.plantly.companyTest.support.CompanyCreateRequestBuilder;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.GradePolicyRegistry;
import project.plantly.domain.company.policy.rule.VideoUrlPolicy;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// VideoUrlPolicy: 동영상 미허용 등급이 videoUrl 을 보내면 거부. 값이 없으면 검증하지 않는다.
@DisplayName("VideoUrlPolicy: 동영상 사용 게이팅")
class VideoUrlPolicyTest {

    private final VideoUrlPolicy policy = new VideoUrlPolicy(new GradePolicyRegistry());

    private final CompanySubscription free = CompanySubscription.freeForUser(LocalDate.now());              // 동영상 불가
    private final CompanySubscription standard = CompanySubscription.active(CompanyGrade.STANDARD, LocalDate.now(), null); // 동영상 허용
    private final CompanySubscription admin = CompanySubscription.adminExempt(LocalDate.now());

    @Test
    @DisplayName("FREE 가 videoUrl 을 보내면 VIDEO_NOT_ALLOWED")
    void free_withVideo_throws() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest()
                .videoUrl("https://youtu.be/x").build();

        assertThatThrownBy(() -> policy.apply(null, request, free))
                .extracting("errorCode")
                .isEqualTo(CompanyErrorCode.VIDEO_NOT_ALLOWED);
    }

    @Test
    @DisplayName("FREE 라도 videoUrl 이 null 이면 통과")
    void free_nullVideo_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().build();

        assertThatCode(() -> policy.apply(null, request, free)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("FREE 라도 videoUrl 이 공백이면 통과")
    void free_blankVideo_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest()
                .videoUrl("   ").build();

        assertThatCode(() -> policy.apply(null, request, free)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("동영상 허용 등급(STANDARD)은 videoUrl 을 보내도 통과")
    void standard_withVideo_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest()
                .videoUrl("https://youtu.be/x").build();

        assertThatCode(() -> policy.apply(null, request, standard)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("관리자 등록은 videoUrl 을 보내도 면제되어 통과")
    void admin_withVideo_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest()
                .videoUrl("https://youtu.be/x").build();

        assertThatCode(() -> policy.apply(null, request, admin)).doesNotThrowAnyException();
    }
}
