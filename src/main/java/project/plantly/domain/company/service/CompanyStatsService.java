package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.category.CategoryService;
import project.plantly.domain.company.certification.CertificationRepository;
import project.plantly.domain.company.dto.CompanyStatsResponse;
import project.plantly.domain.company.enums.CompanyVisibility;
import project.plantly.domain.company.industry.IndustryRepository;
import project.plantly.domain.company.repository.CompanyRepository;

/**
 * 현황 지표 조회. 회사 조회(CompanyQueryService)와 분리한 이유는 다루는 대상이 다르기 때문이다 —
 * 저쪽은 '어떤 회사를 보여줄까'이고 여기는 '전체가 몇인가'다. 접근 제어도 없다(전부 공개 숫자).
 *
 * <p>네 숫자의 출처가 제각각이라 한 곳에 모은다. 프론트의 현황 패널이 이 넷을 함께 그리므로,
 * 흩어 두면 화면 하나가 네 번 왕복하게 된다.
 */
@Service
@RequiredArgsConstructor
public class CompanyStatsService {

    private final CompanyRepository companyRepository;
    private final CategoryService categoryService;
    private final IndustryRepository industryRepository;
    private final CertificationRepository certificationRepository;

    /**
     * 현황 네 숫자. 카테고리는 DB 를 타지 않는다(메모리 스냅샷) — 실제 쿼리는 회사·업종·인증 세 번의
     * {@code count(*)} 뿐이고, 셋 다 목록을 만들지 않으므로 데이터가 늘어도 응답 크기는 그대로다.
     */
    @Transactional(readOnly = true)
    public CompanyStatsResponse getStats() {
        return new CompanyStatsResponse(
                companyRepository.countByDeletedFalseAndVisibility(CompanyVisibility.PUBLIC),
                // 평면 count 가 아니라 공개 트리를 세는 이유는 CategoryService.countPublicTree 주석 참고
                // (비활성 조상 아래의 활성 자식은 화면에 없으므로 세면 안 된다).
                categoryService.countPublicTree(),
                industryRepository.countByActiveTrue(),
                certificationRepository.countByActiveTrue());
    }
}
