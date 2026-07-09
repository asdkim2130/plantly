package project.plantly.domain.company.stat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.repository.CompanyRepository;
import project.plantly.domain.company.stat.dto.FavoriteToggleResponse;
import project.plantly.domain.company.stat.dto.LikeToggleResponse;
import project.plantly.global.exception.BusinessException;

// 유저의 회사 좋아요/즐겨찾기 토글 전담. 원천 행(CompanyLike/CompanyFavorite)만으로 상태를 표현한다.
// 좋아요는 가벼운 on/off 토글(목록 미노출), 즐겨찾기는 유저가 목록으로 관리한다. 취소 = 행 삭제(hard delete).
// 집계 카운터 캐시(CompanyStat)는 두지 않는다 — count 가 필요해지면 원천 행 COUNT(*) 로 파생한다.
@Service
@RequiredArgsConstructor
public class CompanyStatService {

    private final CompanyRepository companyRepository;
    private final CompanyLikeRepository companyLikeRepository;
    private final CompanyFavoriteRepository companyFavoriteRepository;

    // 좋아요 토글. 이미 눌렀으면 취소(행 삭제), 아니면 등록(행 저장).
    // 삭제 반환값으로 '존재 여부'를 판별해 조회 쿼리를 한 번 아낀다.
    @Transactional
    public LikeToggleResponse toggleLike(Long userId, Long companyId) {
        validateActiveCompany(companyId);

        boolean liked;
        if (companyLikeRepository.deleteByUserIdAndCompanyId(userId, companyId) > 0) {
            liked = false;
        } else {
            companyLikeRepository.save(new CompanyLike(userId, companyId));
            liked = true;
        }
        return new LikeToggleResponse(liked);
    }

    // 즐겨찾기 토글. 좋아요와 동일한 on/off 구조 — 목록 노출은 조회 API(findByUserId...)가 별도로 담당한다.
    @Transactional
    public FavoriteToggleResponse toggleFavorite(Long userId, Long companyId) {
        validateActiveCompany(companyId);

        boolean favorited;
        if (companyFavoriteRepository.deleteByUserIdAndCompanyId(userId, companyId) > 0) {
            favorited = false;
        } else {
            companyFavoriteRepository.save(new CompanyFavorite(userId, companyId));
            favorited = true;
        }
        return new FavoriteToggleResponse(favorited);
    }

    // 삭제된 회사는 미존재로 취급(공개 조회와 동일 정책). 없는·삭제된 회사엔 상호작용 불가.
    private void validateActiveCompany(Long companyId) {
        if (!companyRepository.existsByIdAndDeletedFalse(companyId)) {
            throw new BusinessException(CompanyErrorCode.COMPANY_NOT_FOUND);
        }
    }
}
