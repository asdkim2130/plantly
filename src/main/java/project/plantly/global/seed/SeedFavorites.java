package project.plantly.global.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.stat.CompanyFavoriteRepository;

import java.util.List;

/**
 * 즐겨찾기 시드.
 *
 * <p>{@code /companies/favorites} 는 독립된 페이징 뷰라, 회사가 아무리 많아도 즐겨찾기 행이 없으면
 * 빈 목록이다. 회사 정족수와 별개로 여기도 따로 채워야 한다.
 *
 * <p>{@code insertIfAbsent} 를 쓰는 이유는 두 가지다 — unique(user_id, company_id) 충돌에 안전하고,
 * {@code created_at} 을 {@code now()} 로 직접 채워야 하는 네이티브 upsert 라 엔티티 저장과 결과가 같다.
 */
@Slf4j
@Component
@Profile(SeedConfig.PROFILE)
@RequiredArgsConstructor
public class SeedFavorites {

    private final CompanyFavoriteRepository favoriteRepository;

    @Transactional
    public int create(SeedAccount owner, List<Long> companyIds) {
        int inserted = 0;
        for (Long companyId : companyIds) {
            inserted += favoriteRepository.insertIfAbsent(owner.userId(), companyId);
        }
        log.info("[seed] 즐겨찾기 {}건 생성 ({} 계정)", inserted, owner.code());
        return inserted;
    }
}
