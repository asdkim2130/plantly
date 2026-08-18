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
 * 설정된 자리 수를 그대로 넘기는지, 세 레일을 한 번에 개인화하고 원래 경계대로 다시 가르는지.
 *
 * <p>레일이 셋이 되면서 경계가 둘로 늘었다. 가르기가 어긋나면 카드가 엉뚱한 영역에 실려도 예외 없이
 * 조용히 잘못된 화면이 나가므로, 빈 레일이 섞인 경우까지 여기서 잠근다.
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
    private static final int LATEST_SLOTS = 12;

    @BeforeEach
    void injectSlots() {
        // @Value 필드는 Mockito 가 채우지 않는다(설정값이라 주입 대상이 아님).
        ReflectionTestUtils.setField(service, "spotlightSlots", SPOTLIGHT_SLOTS);
        ReflectionTestUtils.setField(service, "featuredSlots", FEATURED_SLOTS);
        ReflectionTestUtils.setField(service, "latestSlots", LATEST_SLOTS);
    }

    @Test
    @DisplayName("설정된 자리 수를 레일별로 그대로 넘기고, 세 레일을 경계대로 갈라 응답한다")
    void passesConfiguredSlots_andSplitsRailsAtBoundary() {
        given(showcaseCardRepository.findSpotlight(SPOTLIGHT_SLOTS))
                .willReturn(new ShowcaseRail(List.of(card(1L), card(2L)), 2));
        given(showcaseCardRepository.findFeatured(FEATURED_SLOTS))
                .willReturn(new ShowcaseRail(List.of(card(3L)), 1));
        given(showcaseCardRepository.findLatest(LATEST_SLOTS))
                .willReturn(new ShowcaseRail(List.of(card(4L), card(5L)), 40));

        CompanyShowcaseResponse response = service.getShowcase(null);

        assertThat(response.spotlight()).extracting(CompanySummary::id).containsExactly(1L, 2L);
        assertThat(response.featured()).extracting(CompanySummary::id).containsExactly(3L);
        assertThat(response.latest()).extracting(CompanySummary::id).containsExactly(4L, 5L);
    }

    @Test
    @DisplayName("최근 등록 레일의 후보 초과는 경고 없이 통과한다 — 이 레일에선 초과가 정상이다")
    void latestRailOverflow_isNotTreatedAsAnomaly() {
        given(showcaseCardRepository.findSpotlight(SPOTLIGHT_SLOTS))
                .willReturn(new ShowcaseRail(List.of(), 0));
        given(showcaseCardRepository.findFeatured(FEATURED_SLOTS))
                .willReturn(new ShowcaseRail(List.of(), 0));
        // 후보 500 건 vs 자리 12 칸. 스팟라이트였다면 경고 대상이지만 여기선 정상 동작이다.
        given(showcaseCardRepository.findLatest(LATEST_SLOTS))
                .willReturn(new ShowcaseRail(List.of(card(9L)), 500));

        CompanyShowcaseResponse response = service.getShowcase(null);

        assertThat(response.latest()).extracting(CompanySummary::id).containsExactly(9L);
    }

    @Test
    @DisplayName("로그인 뷰어면 세 레일을 이어 붙여 한 번만 개인화하고, 카드별 플래그를 제자리에 채운다")
    void enrichesAllRailsInOnePass() {
        given(showcaseCardRepository.findSpotlight(SPOTLIGHT_SLOTS))
                .willReturn(new ShowcaseRail(List.of(card(1L), card(2L)), 2));
        given(showcaseCardRepository.findFeatured(FEATURED_SLOTS))
                .willReturn(new ShowcaseRail(List.of(card(3L)), 1));
        given(showcaseCardRepository.findLatest(LATEST_SLOTS))
                .willReturn(new ShowcaseRail(List.of(card(4L)), 1));
        // 배치 조회는 세 레일을 합친 id 목록으로 각 1회만 일어난다(레일별로 나눠 부르면 2회씩 6회가 된다).
        given(companyLikeRepository.findLikedCompanyIds(eq(7L), anyList())).willReturn(List.of(2L));
        given(companyFavoriteRepository.findFavoritedCompanyIds(eq(7L), anyList())).willReturn(List.of(3L, 4L));

        CompanyShowcaseResponse response = service.getShowcase(7L);

        assertThat(response.spotlight()).extracting(CompanySummary::likedByMe).containsExactly(false, true);
        assertThat(response.featured()).extracting(CompanySummary::favoritedByMe).containsExactly(true);
        // 마지막 레일까지 플래그가 밀리지 않고 제자리에 붙는다.
        assertThat(response.latest()).extracting(CompanySummary::favoritedByMe).containsExactly(true);
    }

    @Test
    @DisplayName("가운데 레일이 비어도 앞뒤 레일이 서로 밀려들어오지 않는다")
    void emptyMiddleRail_doesNotShiftNeighbours() {
        given(showcaseCardRepository.findSpotlight(SPOTLIGHT_SLOTS))
                .willReturn(new ShowcaseRail(List.of(card(1L)), 1));
        given(showcaseCardRepository.findFeatured(FEATURED_SLOTS))
                .willReturn(new ShowcaseRail(List.of(), 0));
        given(showcaseCardRepository.findLatest(LATEST_SLOTS))
                .willReturn(new ShowcaseRail(List.of(card(3L)), 1));

        CompanyShowcaseResponse response = service.getShowcase(null);

        assertThat(response.spotlight()).extracting(CompanySummary::id).containsExactly(1L);
        assertThat(response.featured()).isEmpty();
        assertThat(response.latest()).extracting(CompanySummary::id).containsExactly(3L);
    }

    @Test
    @DisplayName("세 레일이 모두 비면 빈 리스트 셋을 내려준다")
    void allRailsEmpty_returnsEmptyLists() {
        given(showcaseCardRepository.findSpotlight(SPOTLIGHT_SLOTS))
                .willReturn(new ShowcaseRail(List.of(), 0));
        given(showcaseCardRepository.findFeatured(FEATURED_SLOTS))
                .willReturn(new ShowcaseRail(List.of(), 0));
        given(showcaseCardRepository.findLatest(LATEST_SLOTS))
                .willReturn(new ShowcaseRail(List.of(), 0));

        CompanyShowcaseResponse response = service.getShowcase(null);

        assertThat(response.spotlight()).isEmpty();
        assertThat(response.featured()).isEmpty();
        assertThat(response.latest()).isEmpty();
    }

    private CompanySummary card(Long id) {
        return new CompanySummary(id, "회사" + id, null, "logo", "cover", "#2E7D32", "서울 강남구",
                false, false, false, List.of(), List.of(), List.of(), false, false);
    }
}
