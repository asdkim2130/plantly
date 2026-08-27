package project.plantly.companyTest.companyServiceTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.companyTest.support.CompanyCreateRequestBuilder;
import project.plantly.companyTest.support.PostgresContainerTest;
import project.plantly.domain.company.certification.Certification;
import project.plantly.domain.company.certification.CertificationType;
import project.plantly.domain.company.dto.CompanyCreateRequest.CertificationRequest;
import project.plantly.domain.company.entity.Address;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.link.CompanyCertification;
import project.plantly.domain.company.repository.CompanyCertificationRepository;
import project.plantly.domain.company.service.CompanyLinkWriter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 인증 링크의 부분 유니크 인덱스 검증 — 실제 Postgres 에서만 의미가 있는 테스트.
 *
 * <p>H2 로 도는 슬라이스({@code CompanyLinkWriterTest})는 쓰기 경로가 중복을 접어 넣는지까지만 볼 수 있다.
 * "일반 인증은 마스터당 1건, 기타는 이름당 1건"이라는 유일성 자체는 {@code CompanyCertificationIndexInitializer}
 * 가 만든 부분 인덱스가 DB 에서 강제하므로, 그게 실제로 물리는지는 여기서 확인한다.
 */
@Transactional
@DisplayName("인증 링크 유일성: 일반은 마스터당 1건, 기타는 이름당 1건 (Postgres 부분 인덱스)")
class CompanyCertificationUniquenessTest extends PostgresContainerTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CompanyLinkWriter linkWriter;

    @Autowired
    private CompanyCertificationRepository companyCertificationRepository;

    @Test
    @DisplayName("부분 유니크 인덱스 두 개가 실제로 만들어져 있다")
    void partialUniqueIndexesExist() {
        @SuppressWarnings("unchecked")
        List<String> indexNames = em.createNativeQuery(
                        "SELECT indexname FROM pg_indexes WHERE tablename = 'company_certification'")
                .getResultList();

        assertThat(indexNames)
                .contains("ux_company_certification_master", "ux_company_certification_custom");
    }

    @Test
    @DisplayName("같은 마스터 인증을 두 번 붙이면 DB 가 거부한다 (custom_name IS NULL 쪽)")
    void rejectsDuplicateMasterLink() {
        Company company = persistCompany();
        Certification iso = persistCertification("ISO 9001", "iso-9001-t", CertificationType.MANAGEMENT_SYSTEM);
        insertLink(company, iso, null);

        assertThatThrownBy(() -> insertLink(company, iso, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 이름의 기타 인증을 두 번 붙이면 거부하고, 이름이 다르면 허용한다")
    void rejectsDuplicateCustomNameButAllowsDifferentOnes() {
        Company company = persistCompany();
        Certification etc = persistCertification("기타", "etc-t", CertificationType.ETC);
        insertLink(company, etc, "사내 표준 품질인증");

        // 같은 마스터라도 이름이 다르면 별개다 — 이게 (company_id, certification_id) 유니크를 걷어낸 이유다.
        assertThatCode(() -> insertLink(company, etc, "○○협회 우수기업 인증")).doesNotThrowAnyException();

        assertThatThrownBy(() -> insertLink(company, etc, "사내 표준 품질인증"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("직접 입력한 인증명 변경도 교체(PUT) 한 번으로 끝난다 — 별도 삭제 경로가 필요 없다")
    void renameCustomCertificationThroughReplace() {
        Company company = persistCompany();
        Certification iso = persistCertification("ISO 9001", "iso-9001-r", CertificationType.MANAGEMENT_SYSTEM);
        Certification etc = persistCertification("기타", "etc-r", CertificationType.ETC);

        linkWriter.write(company, CompanyCreateRequestBuilder.aRequest()
                .certifications(List.of(
                        new CertificationRequest(iso.getId(), null),
                        new CertificationRequest(etc.getId(), "사내 표준 품질인증")))
                .build());
        em.flush();

        // 이름만 바뀐 교체. 삭제가 INSERT 보다 늦게 나가면 (company, etc, 이름) 인덱스가 여기서 문다.
        linkWriter.replaceCertifications(company, List.of(
                new CertificationRequest(iso.getId(), null),
                new CertificationRequest(etc.getId(), "사내 표준 품질인증 QM-2024")));
        em.flush();

        assertThat(displayNames(company)).containsExactly("ISO 9001", "사내 표준 품질인증 QM-2024");

        // 직접 입력을 전부 걷어내고 마스터 인증만 남기는 것도 같은 교체 한 번이다.
        linkWriter.replaceCertifications(company, List.of(new CertificationRequest(iso.getId(), null)));
        em.flush();

        assertThat(displayNames(company)).containsExactly("ISO 9001");
    }

    private List<String> displayNames(Company company) {
        em.clear();
        return companyCertificationRepository.findLinksByCompanyId(company.getId())
                .stream().map(CompanyCertification::displayName).toList();
    }

    // 쓰기 경로(중복 제거)를 우회해 DB 제약만 때린다.
    private void insertLink(Company company, Certification certification, String customName) {
        companyCertificationRepository.saveAndFlush(
                new CompanyCertification(company, certification, customName, 0));
    }

    private Company persistCompany() {
        Company company = Company.createByUser(1L, null, "회사", "대표", null,
                Address.of("06236", "서울 강남구", null, "테헤란로 1"), null, "logo", null,
                null, null, null, null, null, null, null, null);
        em.persist(company);
        em.flush();
        return company;
    }

    private Certification persistCertification(String name, String slug, CertificationType type) {
        Certification certification = Certification.create(name, slug, type, 0);
        em.persist(certification);
        em.flush();
        return certification;
    }
}
