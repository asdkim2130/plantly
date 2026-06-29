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
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.service.CompanyLinkWriter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// CompanyLinkWriter 가 링크의 displayOrder 를 "회사가 보낸 요청(선택) 순서"로 부여하는지 검증.
// findAllById 는 입력 순서를 보존하지 않으므로, id 오름차순과 다른 요청 순서로 그 버그를 막았는지 확인한다.
// 순수 JPA 로직이라 H2(@DataJpaTest)로 충분하다.
@DataJpaTest
@ActiveProfiles("test")
@Import(CompanyLinkWriter.class)
@DisplayName("CompanyLinkWriter: 링크 displayOrder = 요청(선택) 순서")
class CompanyLinkWriterTest {

    @Autowired EntityManager em;
    @Autowired CompanyLinkWriter linkWriter;

    @Test
    @DisplayName("카테고리 링크의 displayOrder 는 id 순이 아니라 요청에 담긴 순서를 따른다")
    void assignsDisplayOrderByRequestOrder() {
        Category c1 = persistRoot("A", "CAT-A");
        Category c2 = persistRoot("B", "CAT-B");
        Category c3 = persistRoot("C", "CAT-C");

        Company company = Company.createByUser(1L, null, "회사", "대표", null,
                "06236", "서울 강남구", "테헤란로 1", null, "logo",
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
    }

    private Category persistRoot(String name, String code) {
        Category category = Category.createRoot(name, code, null, null, 0);
        em.persist(category);
        return category;
    }
}
