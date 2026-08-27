package project.plantly.companyTest.companyServiceTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.plantly.domain.company.category.CategoryService;
import project.plantly.domain.company.certification.CertificationRepository;
import project.plantly.domain.company.dto.CompanyStatsResponse;
import project.plantly.domain.company.enums.CompanyVisibility;
import project.plantly.domain.company.industry.IndustryRepository;
import project.plantly.domain.company.repository.CompanyRepository;
import project.plantly.domain.company.service.CompanyStatsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 현황 지표 조립. 각 숫자를 어떻게 세는지는 리포지토리 파생 쿼리와 CategoryServiceTest 가 잠그고,
 * 여기서는 <b>어떤 기준으로 물어보는지</b>를 본다 — 현황이 공개 목록과 다른 조건을 쓰면
 * "업종 24개"라고 해놓고 드롭다운에는 20개만 있는 화면이 나온다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyStatsService: 메인 현황 지표")
class CompanyStatsServiceTest {

    @Mock CompanyRepository companyRepository;
    @Mock CategoryService categoryService;
    @Mock IndustryRepository industryRepository;
    @Mock CertificationRepository certificationRepository;

    @InjectMocks CompanyStatsService service;

    @Test
    @DisplayName("네 숫자를 각 출처에서 모아 그대로 싣는다")
    void collectsFourCounts() {
        given(companyRepository.countByDeletedFalseAndVisibility(CompanyVisibility.PUBLIC)).willReturn(45L);
        given(categoryService.countPublicTree()).willReturn(143L);
        given(industryRepository.countByActiveTrue()).willReturn(24L);
        given(certificationRepository.countByActiveTrue()).willReturn(8L);

        CompanyStatsResponse stats = service.getStats();

        assertThat(stats.companyCount()).isEqualTo(45);
        assertThat(stats.categoryCount()).isEqualTo(143);
        assertThat(stats.industryCount()).isEqualTo(24);
        assertThat(stats.certificationCount()).isEqualTo(8);
    }

    @Test
    @DisplayName("회사 수는 공개·미삭제만 센다 — 비공개/삭제 회사가 규모로 잡히면 안 된다")
    void companyCountUsesPublicVisibilityOnly() {
        given(companyRepository.countByDeletedFalseAndVisibility(CompanyVisibility.PUBLIC)).willReturn(45L);
        given(categoryService.countPublicTree()).willReturn(0L);
        given(industryRepository.countByActiveTrue()).willReturn(0L);
        given(certificationRepository.countByActiveTrue()).willReturn(0L);

        service.getStats();

        // showcase 레일의 가시성 규칙과 같은 조건이라는 것이 계약이다.
        verify(companyRepository).countByDeletedFalseAndVisibility(CompanyVisibility.PUBLIC);
    }

    @Test
    @DisplayName("카테고리는 평면 count 가 아니라 공개 트리를 센다")
    void categoryCountComesFromPublicTree() {
        given(companyRepository.countByDeletedFalseAndVisibility(CompanyVisibility.PUBLIC)).willReturn(0L);
        given(categoryService.countPublicTree()).willReturn(143L);
        given(industryRepository.countByActiveTrue()).willReturn(0L);
        given(certificationRepository.countByActiveTrue()).willReturn(0L);

        service.getStats();

        // 비활성 조상 아래의 활성 자식을 빼려면 트리를 거쳐야 한다(CategoryServiceTest 가 그 규칙을 잠근다).
        // 여기서 리포지토리 count 로 갈아타면 그 규칙이 조용히 무력화된다.
        verify(categoryService).countPublicTree();
    }

    @Test
    @DisplayName("아무것도 없으면 0 네 개를 내려준다 — 빈 상태에서도 응답이 비지 않는다")
    void emptyPlatform_returnsZeros() {
        given(companyRepository.countByDeletedFalseAndVisibility(CompanyVisibility.PUBLIC)).willReturn(0L);
        given(categoryService.countPublicTree()).willReturn(0L);
        given(industryRepository.countByActiveTrue()).willReturn(0L);
        given(certificationRepository.countByActiveTrue()).willReturn(0L);

        CompanyStatsResponse stats = service.getStats();

        assertThat(stats).isEqualTo(new CompanyStatsResponse(0, 0, 0, 0));
    }
}
