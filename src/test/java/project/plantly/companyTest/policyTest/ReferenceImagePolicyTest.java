package project.plantly.companyTest.policyTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import project.plantly.companyTest.support.CompanyCreateRequestBuilder;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ReferenceRequest;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.GradePolicyRegistry;
import project.plantly.domain.company.policy.rule.ReferenceImagePolicy;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ReferenceImagePolicy: 레퍼런스 이미지 업로드 허용 여부(0=비활성)와 1건당 장수 상한을 등급별로 강제.
@DisplayName("ReferenceImagePolicy: 레퍼런스 이미지 게이팅/상한")
class ReferenceImagePolicyTest {

    private final ReferenceImagePolicy policy = new ReferenceImagePolicy(new GradePolicyRegistry());

    private final CompanySubscription free = CompanySubscription.freeForUser(LocalDate.now());              // 0 = 업로드 비활성
    private final CompanySubscription enterprise = CompanySubscription.active(CompanyGrade.ENTERPRISE, LocalDate.now(), null); // 1건당 10장
    private final CompanySubscription admin = CompanySubscription.adminExempt(LocalDate.now());

    @Test
    @DisplayName("업로드 비활성 등급(FREE)이 레퍼런스 이미지를 보내면 REFERENCE_IMAGE_NOT_ALLOWED")
    void free_withImages_throwsNotAllowed() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().referenceWithImages(1).build();

        assertThatThrownBy(() -> policy.apply(null, request, free))
                .extracting("errorCode")
                .isEqualTo(CompanyErrorCode.REFERENCE_IMAGE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("FREE 라도 이미지 없는 레퍼런스는 통과")
    void free_referenceWithoutImages_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest()
                .references(List.of(new ReferenceRequest("프로젝트", null, null, null, List.of()))).build();

        assertThatCode(() -> policy.apply(null, request, free)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ENTERPRISE 1건당 상한(10) 이내면 통과")
    void enterprise_withinLimit_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().referenceWithImages(10).build();

        assertThatCode(() -> policy.apply(null, request, enterprise)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ENTERPRISE 1건당 상한(10) 초과면 REFERENCE_IMAGE_LIMIT_EXCEEDED")
    void enterprise_overLimit_throws() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().referenceWithImages(11).build();

        assertThatThrownBy(() -> policy.apply(null, request, enterprise))
                .extracting("errorCode")
                .isEqualTo(CompanyErrorCode.REFERENCE_IMAGE_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("references 가 null 이면 통과")
    void nullReferences_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().build();

        assertThatCode(() -> policy.apply(null, request, free)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("관리자 등록은 레퍼런스 이미지를 보내도 면제되어 통과")
    void admin_exempt_passes() {
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest().referenceWithImages(5).build();

        assertThatCode(() -> policy.apply(null, request, admin)).doesNotThrowAnyException();
    }
}
