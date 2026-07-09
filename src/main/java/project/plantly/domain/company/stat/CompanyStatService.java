package project.plantly.domain.company.stat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.repository.CompanyRepository;
import project.plantly.domain.company.stat.dto.FavoriteToggleResponse;
import project.plantly.domain.company.stat.dto.LikeToggleResponse;
import project.plantly.global.exception.BusinessException;

// 유저의 회사 좋아요/즐겨찾기 토글 + 회사 집계 카운터(CompanyStat) 동기화 전담.
// 좋아요는 가벼운 on/off 토글(목록 미노출), 즐겨찾기는 유저가 목록으로 관리한다. 취소 = 행 삭제(hard delete).
// 상호작용 행(CompanyLikes/Favorite)과 카운터(CompanyStat)를 한 트랜잭션으로 묶어 항상 함께 증감시킨다.
@Service
@RequiredArgsConstructor
public class CompanyStatService {

    private final CompanyRepository companyRepository;
    private final CompanyStatRepository companyStatRepository;
    private final CompanyLikeRepository companyLikeRepository;
    private final CompanyFavoriteRepository companyFavoriteRepository;

    // 좋아요 토글. 이미 눌렀으면 취소(행 삭제 + 카운터 -1), 아니면 등록(행 저장 + 카운터 +1).
    // 삭제 반환값으로 '존재 여부'를 판별해 조회 쿼리를 한 번 아낀다.
    @Transactional
    public LikeToggleResponse toggleLike(Long userId, Long companyId) {
        validateActiveCompany(companyId);
        CompanyStat stat = getOrCreateStat(companyId);

        boolean liked;
        if (companyLikeRepository.deleteByUserIdAndCompanyId(userId, companyId) > 0) {
            stat.decreaseLike();
            liked = false;
        } else {
            companyLikeRepository.save(new CompanyLike(userId, companyId));
            stat.increaseLike();
            liked = true;
        }
        return new LikeToggleResponse(liked, stat.getLikeCount());
    }

    // 즐겨찾기 토글. 좋아요와 동일한 on/off 구조 — 목록 노출은 조회 API(findByUserId...)가 별도로 담당한다.
    @Transactional
    public FavoriteToggleResponse toggleFavorite(Long userId, Long companyId) {
        validateActiveCompany(companyId);
        CompanyStat stat = getOrCreateStat(companyId);

        boolean favorited;
        if (companyFavoriteRepository.deleteByUserIdAndCompanyId(userId, companyId) > 0) {
            stat.decreaseFavorite();
            favorited = false;
        } else {
            companyFavoriteRepository.save(new CompanyFavorite(userId, companyId));
            stat.increaseFavorite();
            favorited = true;
        }
        return new FavoriteToggleResponse(favorited, stat.getFavoriteCount());
    }

    // 삭제된 회사는 미존재로 취급(공개 조회와 동일 정책). 없는·삭제된 회사엔 상호작용 불가.
    private void validateActiveCompany(Long companyId) {
        if (!companyRepository.existsByIdAndDeletedFalse(companyId)) {
            throw new BusinessException(CompanyErrorCode.COMPANY_NOT_FOUND);
        }
    }

    // 집계 행은 회사 등록 시(CompanyService.persist) 함께 생성되므로 보통은 findByCompanyId 로 바로 찾힌다.
    // orElseGet 은 이 변경 이전에 등록된 레거시 회사(집계 행 없음)를 위한 안전망이다.
    private CompanyStat getOrCreateStat(Long companyId) {
        return companyStatRepository.findByCompanyId(companyId)
                .orElseGet(() -> companyStatRepository.save(CompanyStat.init(companyId)));
    }
}
