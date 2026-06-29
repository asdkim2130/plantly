package project.plantly.companyTest.searchTest;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.companyTest.support.PostgresContainerTest;
import project.plantly.domain.company.category.Category;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.link.CompanyCategory;
import project.plantly.domain.company.search.CompanySearchDocumentWriter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 회사 검색 API 통합(Postgres). HTTP→컨트롤러→서비스→PG 전 구간 + 익명 접근 + 쿼리 파라미터 바인딩을
 * 실제 스택으로 검증한다. (쿼리 변형 자체의 정확성은 PostgresTrigramCompanySearchTest 가 담당.)
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("회사 검색 API 통합(Postgres): GET /api/v1/companies")
class CompanySearchApiTest extends PostgresContainerTest {

    @Autowired MockMvc mockMvc;
    @Autowired EntityManager em;
    @Autowired CompanySearchDocumentWriter writer;

    @Test
    @DisplayName("통합 키워드로 매칭된 회사만 페이징 반환 (인증 없이 200)")
    void searchByKeyword() throws Exception {
        index(persistCompany("플랜틀리", "스마트팜 자동관수"));
        index(persistCompany("베타기계", "금속 절삭 가공"));

        mockMvc.perform(get("/api/v1/companies").param("keyword", "스마트팜"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].companyName").value("플랜틀리"))
                .andExpect(jsonPath("$.data.pageInfo.totalElement").value(1));
    }

    @Test
    @DisplayName("categoryIds 파라미터(List 바인딩) → 대분류 선택 시 후손 서브트리까지 매칭")
    void searchByCategorySubtree() throws Exception {
        Category root = Category.createRoot("제조", "MFG", null, null, 0);
        em.persist(root);
        Category child = Category.createChild(root, "정밀가공", "MFG-P", null, null, 0);
        em.persist(child);

        Company a = persistCompany("가가", "x");
        em.persist(new CompanyCategory(a, child));   // 중분류 연결 → closure 에 root 포함
        index(a);
        index(persistCompany("나나", "y"));            // 카테고리 없음

        mockMvc.perform(get("/api/v1/companies").param("categoryIds", String.valueOf(root.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].companyName").value("가가"));
    }

    private Company persistCompany(String name, String content) {
        Company c = Company.createByUser(1L, null, name, "대표자", null,
                "06236", "서울 강남구", "테헤란로 1", null, "logo-" + name,
                name + " 요약", content, null, null, null, null, null, null);
        em.persist(c);
        return c;
    }

    private Company index(Company c) {
        writer.write(c.getId());
        return c;
    }
}
