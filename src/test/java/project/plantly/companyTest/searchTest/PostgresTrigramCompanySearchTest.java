package project.plantly.companyTest.searchTest;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.companyTest.support.PostgresContainerTest;
import project.plantly.domain.company.category.Category;
import project.plantly.domain.company.certification.Certification;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanyTag;
import project.plantly.domain.company.entity.link.CompanyCategory;
import project.plantly.domain.company.entity.link.CompanyCertification;
import project.plantly.domain.company.entity.link.CompanyIndustry;
import project.plantly.domain.company.industry.Industry;
import project.plantly.domain.company.search.CompanySearchCriteria;
import project.plantly.domain.company.search.CompanySearchCriteria.AdvancedText;
import project.plantly.domain.company.search.CompanySearchDocumentWriter;
import project.plantly.domain.company.search.CompanySearchRepository;
import project.plantly.domain.company.search.dto.CompanySummary;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("PostgresTrigramCompanySearch: 네이티브 검색")
class PostgresTrigramCompanySearchTest extends PostgresContainerTest {

    @Autowired EntityManager em;
    @Autowired CompanySearchDocumentWriter writer;
    @Autowired CompanySearchRepository repository;

    @Test
    @DisplayName("통합 키워드: search_all 부분일치, 다중 토큰은 AND")
    void unifiedKeyword() {
        Company a = index(persistCompany("플랜틀리", "스마트팜 자동관수 시스템"));
        index(persistCompany("베타기계", "금속 절삭 가공"));

        assertThat(ids(search(kw("스마트팜")))).containsExactly(a.getId());
        assertThat(ids(search(kw("스마트팜 자동관수")))).containsExactly(a.getId()); // 둘 다 A 에 있음
        assertThat(ids(search(kw("스마트팜 절삭")))).isEmpty();                      // 한 회사에 둘 다 있진 않음
    }

    @Test
    @DisplayName("고급 검색: 지정한 필드(회사명) 컬럼에만 매칭한다")
    void advancedTargetsColumn() {
        Company a = index(persistCompany("플랜틀리", "일반 제조"));
        index(persistCompany("베타기계", "플랜틀리 협력사"));   // '플랜틀리' 는 본문에만

        AdvancedText byName = new AdvancedText("플랜틀리", null, null, null, null, null, null, null, null);
        assertThat(ids(search(new CompanySearchCriteria(null, byName, null, null, null))))
                .containsExactly(a.getId());
    }

    @Test
    @DisplayName("카테고리 패싯: 대분류 선택 시 후손 서브트리(closure)까지 매칭한다")
    void categorySubtreeFacet() {
        Category root = Category.createRoot("제조", "MFG", null, null, 0);
        em.persist(root);
        Category child = Category.createChild(root, "정밀가공", "MFG-P", null, null, 0);
        em.persist(child);
        Category otherRoot = Category.createRoot("서비스", "SVC", null, null, 1);
        em.persist(otherRoot);

        Company a = persistCompany("가가", "x");
        em.persist(new CompanyCategory(a, child, 0));      // 중분류 연결 → closure 에 root 포함
        index(a);
        Company b = persistCompany("나나", "y");
        em.persist(new CompanyCategory(b, otherRoot, 0));  // 다른 대분류
        index(b);

        assertThat(ids(search(new CompanySearchCriteria(null, null, null, null, List.of(root.getId())))))
                .containsExactly(a.getId());
    }

    @Test
    @DisplayName("인증 패싯: 선택한 인증에 링크된 회사만 매칭한다")
    void certificationFacet() {
        Certification iso = Certification.create("ISO9001", 0);
        em.persist(iso);

        Company a = persistCompany("가가", "x");
        em.persist(new CompanyCertification(a, iso, 0));
        index(a);
        index(persistCompany("나나", "y"));

        assertThat(ids(search(new CompanySearchCriteria(null, null, List.of(iso.getId()), null, null))))
                .containsExactly(a.getId());
    }

    @Test
    @DisplayName("기본 정렬: 스팟라이트 → 추천 → 최신 (플래그가 생성시간을 이긴다)")
    void defaultSort() {
        Company spot = persistCompany("스팟", "x");   // 가장 먼저 생성(가장 오래됨)
        spot.activateSpotlight();
        index(spot);
        Company feat = persistCompany("추천", "y");
        feat.feature();
        index(feat);
        Company plain = index(persistCompany("일반", "z")); // 가장 최신

        assertThat(ids(search(new CompanySearchCriteria(null, null, null, null, null))))
                .containsExactly(spot.getId(), feat.getId(), plain.getId());
    }

    @Test
    @DisplayName("카드에 직접 연결한 카테고리/태그/산업군 이름을 display_order 순으로 집계 (closure 조상 제외)")
    void summaryAggregatesNames() {
        Category root = Category.createRoot("제조", "MFG", null, null, 0);
        em.persist(root);
        Category precision = Category.createChild(root, "정밀가공", "MFG-P", null, null, 0);
        em.persist(precision);
        Category assembly = Category.createChild(root, "조립", "MFG-A", null, null, 0);
        em.persist(assembly);
        Industry industry = Industry.create("농업기술", "AGRI", null, null, 0);
        em.persist(industry);

        Company a = persistCompany("가가", "x");
        // 링크 displayOrder(=회사가 고른 순서)로 정렬됨 — 이름순(정밀가공<조립)이 아니라 선택 순서(조립=0, 정밀가공=1)를 따른다.
        em.persist(new CompanyCategory(a, precision, 1)); // 직접 링크 = 중분류만 (대분류 제조는 closure 에만)
        em.persist(new CompanyCategory(a, assembly, 0));
        em.persist(new CompanyIndustry(a, industry, 0));
        em.persist(new CompanyTag(a, "스마트팜", 0));
        em.persist(new CompanyTag(a, "IoT", 1));
        index(a);

        CompanySummary card = search(new CompanySearchCriteria(null, null, null, null, null))
                .getContent().get(0);

        assertThat(card.categoryNames()).containsExactly("조립", "정밀가공"); // 링크 displayOrder 0,1 (이름순 아님), 조상 제외
        assertThat(card.tagNames()).containsExactly("스마트팜", "IoT");        // display_order 순
        assertThat(card.industryNames()).containsExactly("농업기술");
    }

    // ===== helpers =====

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

    private Page<CompanySummary> search(CompanySearchCriteria criteria) {
        return repository.search(criteria, PageRequest.of(0, 20));
    }

    private CompanySearchCriteria kw(String keyword) {
        return new CompanySearchCriteria(keyword, null, null, null, null);
    }

    private List<Long> ids(Page<CompanySummary> page) {
        return page.getContent().stream().map(CompanySummary::id).toList();
    }
}
