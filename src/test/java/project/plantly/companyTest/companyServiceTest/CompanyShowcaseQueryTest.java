package project.plantly.companyTest.companyServiceTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import project.plantly.domain.company.dto.CompanyShowcaseResponse;
import project.plantly.domain.company.repository.ShowcaseCardRepository;
import project.plantly.domain.company.repository.ShowcaseCardRepository.ShowcaseRail;
import project.plantly.domain.company.search.dto.CompanySummary;
import project.plantly.domain.company.service.CompanyQueryService;
import project.plantly.domain.company.stat.CompanyFavoriteRepository;
import project.plantly.domain.company.stat.CompanyLikeRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * 메인 화면 레일 조립. 자격 판정은 리포지토리(ShowcaseCardRepositoryTest)가 맡고, 여기선 서비스 몫만 본다 —
 * 설정된 자리 수를 그대로 넘기는지, 두 레일을 한 번에 개인화하고 원래 경계대로 다시 가르는지.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyQueryService.getShowcase: 레일 조립")
class CompanyShowcaseQueryTest {

    @Mock ShowcaseCardRepository showcaseCardRepository;
    @Mock CompanyLikeRepository companyLikeRepository;
    @Mock CompanyFavoriteRepository companyFavoriteRepository;
    @InjectMocks CompanyQueryService service;

    private static final int SPOTLIGHT_SLOTS = 5;
    private static final int FEATURED_SLOTS = 8;

    @BeforeEach
    void injectSlots() {
        // @Value 필드는 Mockito 가 채우지 않는다(설정값이라 주입 대상이 아님).
        ReflectionTestUtils.setField(service, "spotlightSlots", SPOTLIGHT_SLOTS);
        ReflectionTestUtils.setField(service, "featuredSlots", FEATURED_SLOTS);
    }

    @Test
    @DisplayName("설정된 자리 수를 레일별로 그대로 넘기고, 두 레일을 경계대로 갈라 응답한다")
    void passesConfiguredSlots_andSplitsRailsAtBoundary() {
        given(showcaseCardRepository.findSpotlight(SPOTLIGHT_SLOTS))
                .willReturn(new ShowcaseRail(List.of(card(1L), card(2L)), 2));
        given(showcaseCardRepository.findFeatured(FEATURED_SLOTS))
                .willReturn(new ShowcaseRail(List.of(card(3L)), 1));

        CompanyShowcaseResponse response = service.getShowcase(null);

        assertThat(response.spotlight()).extracting(CompanySummary::id).containsExactly(1L, 2L);
        assertThat(response.featured()).extracting(CompanySummary::id).containsExactly(3L);
    }

    @Test
    @DisplayName("로그인 뷰어면 두 레일을 이어 붙여 한 번만 개인화하고, 카드별 플래그를 제자리에 채운다")
    void enrichesBothRailsInOnePass() {
        given(showcaseCardRepository.findSpotlight(SPOTLIGHT_SLOTS))
                .willReturn(new ShowcaseRail(List.of(card(1L), card(2L)), 2));
        given(showcaseCardRepository.findFeatured(FEATURED_SLOTS))
                .willReturn(new ShowcaseRail(List.of(card(3L)), 1));
        // 배치 조회는 두 레일을 합친 id 목록으로 각 1회만 일어난다(레일별로 나눠 부르면 2회씩 4회가 된다).
        given(companyLikeRepository.findLikedCompanyIds(eq(7L), anyList())).willReturn(List.of(2L));
        given(companyFavoriteRepository.findFavoritedCompanyIds(eq(7L), anyList())).willReturn(List.of(3L));

        CompanyShowcaseResponse response = service.getShowcase(7L);

        assertThat(response.spotlight()).extracting(CompanySummary::likedByMe).containsExactly(false, true);
        assertThat(response.featured()).extracting(CompanySummary::favoritedByMe).containsExactly(true);
    }

    @Test
    @DisplayName("한쪽 레일이 비어도 나머지 레일이 밀리지 않는다")
    void emptyRail_doesNotShiftTheOther() {
        given(showcaseCardRepository.findSpotlight(SPOTLIGHT_SLOTS))
                .willReturn(new ShowcaseRail(List.of(), 0));
        given(showcaseCardRepository.findFeatured(FEATURED_SLOTS))
                .willReturn(new ShowcaseRail(List.of(card(3L)), 1));

        CompanyShowcaseResponse response = service.getShowcase(null);

        assertThat(response.spotlight()).isEmpty();
        assertThat(response.featured()).extracting(CompanySummary::id).containsExactly(3L);
    }

    private CompanySummary card(Long id) {
        return new CompanySummary(id, "회사" + id, null, "logo", "서울 강남구",
                false, false, false, List.of(), List.of(), List.of(), false, false);
    }
}
