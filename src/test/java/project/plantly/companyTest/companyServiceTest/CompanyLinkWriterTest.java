package project.plantly.companyTest.companyServiceTest;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import project.plantly.companyTest.support.CompanyCreateRequestBuilder;
import project.plantly.domain.company.category.Category;
import project.plantly.domain.company.certification.Certification;
import project.plantly.domain.company.certification.CertificationType;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.CertificationRequest;
import project.plantly.domain.company.entity.Address;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.link.CompanyCertification;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.repository.CompanyCategoryRepository;
import project.plantly.domain.company.repository.CompanyCertificationRepository;
import project.plantly.domain.company.service.CompanyLinkWriter;
import project.plantly.global.exception.BusinessException;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// CompanyLinkWriter 가 링크의 displayOrder 를 "회사가 보낸 요청(선택) 순서"로 부여하는지 검증.
// findAllById 는 입력 순서를 보존하지 않으므로, id 오름차순과 다른 요청 순서로 그 버그를 막았는지 확인한다.
// 순수 JPA 로직이라 H2(@DataJpaTest)로 충분하다.
@DataJpaTest
@ActiveProfiles("test")
@Import(CompanyLinkWriter.class)
@DisplayName("CompanyLinkWriter: 링크 displayOrder = 요청(선택) 순서 / 인증 직접입력 규약")
class CompanyLinkWriterTest {

    @Autowired EntityManager em;
    @Autowired CompanyLinkWriter linkWriter;
    @Autowired CompanyCategoryRepository companyCategoryRepository;
    @Autowired CompanyCertificationRepository companyCertificationRepository;

    @Test
    @DisplayName("카테고리 링크의 displayOrder 는 id 순이 아니라 요청에 담긴 순서를 따른다")
    void assignsDisplayOrderByRequestOrder() {
        Category c1 = persistRoot("A", "CAT-A");
        Category c2 = persistRoot("B", "CAT-B");
        Category c3 = persistRoot("C", "CAT-C");

        Company company = Company.createByUser(1L, null, "회사", "대표", null,
                Address.of("06236", "서울 강남구", null, "테헤란로 1"), null, "logo", null,
                null, null, null, null, null, null, null, null);
        em.persist(company);

        // 요청 순서를 id 오름차순(c1,c2,c3)과 다르게: [c3, c1, c2]
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest()
                .categoryIds(List.of(c3.getId(), c1.getId(), c2.getId()))
                .build();
        linkWriter.write(company, request);
        em.flush();

        List<Long> orderedCategoryIds = em.createNativeQuery(
                        "SELECT category_id FROM company_category WHERE company_id = :cid ORDER BY display_order")
                .setParameter("cid", company.getId())
                .getResultList().stream()
                .map(o -> ((Number) o).longValue())
                .toList();

        assertThat(orderedCategoryIds).containsExactly(c3.getId(), c1.getId(), c2.getId());

        // 상세 조회(CompanyAggregateLoader)가 쓰는 프로젝션도 displayOrder(=선택 순서) 순으로 반환한다.
        List<Long> projectedCategoryIds = companyCategoryRepository.findLinksByCompanyId(company.getId())
                .stream().map(link -> link.getCategory().getId()).toList();
        assertThat(projectedCategoryIds).containsExactly(c3.getId(), c1.getId(), c2.getId());
    }

    @Test
    @DisplayName("'기타' 인증은 이름만 다르면 같은 마스터에 여러 건 붙고, 표시 이름은 입력값이다")
    void attachesMultipleCustomCertifications() {
        Certification iso = persistCertification("ISO 9001", "iso-9001", CertificationType.MANAGEMENT_SYSTEM);
        Certification etc = persistCertification("기타", "etc", CertificationType.ETC);
        Company company = persistCompany();

        linkWriter.write(company, CompanyCreateRequestBuilder.aRequest()
                .certifications(List.of(
                        new CertificationRequest(etc.getId(), "사내 표준 품질인증"),
                        new CertificationRequest(iso.getId(), null),
                        new CertificationRequest(etc.getId(), "○○협회 우수기업 인증")))
                .build());
        em.flush();

        // 표시 이름은 마스터의 "기타"가 아니라 입력값이고, 순서는 요청 순서를 따른다.
        assertThat(companyCertificationRepository.findLinksByCompanyId(company.getId()))
                .extracting(CompanyCertification::displayName)
                .containsExactly("사내 표준 품질인증", "ISO 9001", "○○협회 우수기업 인증");
    }

    @Test
    @DisplayName("같은 (인증, 이름) 조합은 공백만 다르더라도 1건으로 접힌다")
    void deduplicatesByIdAndCustomName() {
        Certification etc = persistCertification("기타", "etc", CertificationType.ETC);
        Company company = persistCompany();

        linkWriter.write(company, CompanyCreateRequestBuilder.aRequest()
                .certifications(List.of(
                        new CertificationRequest(etc.getId(), "사내 표준 품질인증"),
                        new CertificationRequest(etc.getId(), "  사내 표준 품질인증  ")))
                .build());
        em.flush();

        assertThat(companyCertificationRepository.findLinksByCompanyId(company.getId()))
                .extracting(CompanyCertification::displayName)
                .containsExactly("사내 표준 품질인증");
    }

    @Test
    @DisplayName("'기타' 인증을 이름 없이 보내면 거부한다")
    void rejectsEtcWithoutCustomName() {
        Certification etc = persistCertification("기타", "etc", CertificationType.ETC);
        Company company = persistCompany();

        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest()
                .certifications(List.of(new CertificationRequest(etc.getId(), "   ")))
                .build();

        assertThatThrownBy(() -> linkWriter.write(company, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.CERTIFICATION_CUSTOM_NAME_REQUIRED);
    }

    @Test
    @DisplayName("'기타'가 아닌 인증에 직접 입력한 이름을 붙이면 거부한다")
    void rejectsCustomNameOnMasterCertification() {
        Certification iso = persistCertification("ISO 9001", "iso-9001", CertificationType.MANAGEMENT_SYSTEM);
        Company company = persistCompany();

        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest()
                .certifications(List.of(new CertificationRequest(iso.getId(), "내 마음대로 이름")))
                .build();

        assertThatThrownBy(() -> linkWriter.write(company, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.CERTIFICATION_CUSTOM_NAME_NOT_ALLOWED);
    }

    @Test
    @DisplayName("이미 붙어 있는 항목을 그대로 둔 채 교체해도 유일성 제약에 걸리지 않는다")
    void replaceKeepingExistingLink() {
        Certification iso = persistCertification("ISO 9001", "iso-9001", CertificationType.MANAGEMENT_SYSTEM);
        Certification kc = persistCertification("KC 인증", "kc", CertificationType.MARKET_ACCESS);
        Category category = persistRoot("A", "CAT-A");
        Company company = persistCompany();

        linkWriter.write(company, CompanyCreateRequestBuilder.aRequest()
                .categoryIds(List.of(category.getId()))
                .certifications(List.of(new CertificationRequest(iso.getId(), null)))
                .build());
        em.flush();

        // ISO 는 그대로 두고 KC 만 추가하는, 화면에서 가장 흔한 교체.
        // 삭제가 flush 되기 전에 INSERT 가 나가면 여기서 제약 위반으로 터진다.
        linkWriter.replaceCertifications(company, List.of(
                new CertificationRequest(iso.getId(), null), new CertificationRequest(kc.getId(), null)));
        linkWriter.replaceCategories(company, List.of(category.getId()));
        em.flush();

        assertThat(companyCertificationRepository.findLinksByCompanyId(company.getId()))
                .extracting(CompanyCertification::displayName)
                .containsExactly("ISO 9001", "KC 인증");
        assertThat(companyCategoryRepository.findLinksByCompanyId(company.getId())).hasSize(1);
    }

    @Test
    @DisplayName("null 원소가 들어와도 NPE 가 아니라 비즈니스 예외(400)로 끊는다")
    void rejectsNullElementWithoutNpe() {
        Company company = persistCompany();

        // 컨트롤러 앞단 검증을 뚫고 들어오는 경우(내부 호출·검증 누락)까지 대비한 방어선.
        CompanyCreateRequest request = CompanyCreateRequestBuilder.aRequest()
                .certifications(Collections.singletonList(null))
                .build();

        assertThatThrownBy(() -> linkWriter.write(company, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CompanyErrorCode.CERTIFICATION_NOT_FOUND);
    }

    private Certification persistCertification(String name, String slug, CertificationType type) {
        Certification certification = Certification.create(name, slug, type, 0);
        em.persist(certification);
        return certification;
    }

    private Company persistCompany() {
        Company company = Company.createByUser(1L, null, "회사", "대표", null,
                Address.of("06236", "서울 강남구", null, "테헤란로 1"), null, "logo", null,
                null, null, null, null, null, null, null, null);
        em.persist(company);
        return company;
    }

    private Category persistRoot(String name, String code) {
        Category category = Category.createRoot(name, code, null, null, 0);
        em.persist(category);
        return category;
    }
}
