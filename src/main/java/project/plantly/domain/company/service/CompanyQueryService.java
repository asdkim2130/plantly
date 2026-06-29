package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.dto.CompanyDetailResponse;
import project.plantly.domain.company.dto.CompanyPublicResponse;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.repository.CompanyMemberRepository;
import project.plantly.domain.company.repository.CompanyRepository;
import project.plantly.domain.company.search.CompanySearchCriteria;
import project.plantly.domain.company.search.CompanySearchRepository;
import project.plantly.domain.company.search.dto.CompanySummary;
import project.plantly.global.PageResponse;
import project.plantly.global.exception.BusinessException;

// 회사 상세 조회 전담 서비스. 등록(CompanyService)과 분리한 읽기 전용 경로.
// 세 진입점(공개 / 소유자 / 관리자)은 '누가 무엇을 볼 수 있는가'(접근 제어 + 응답 형태)만 다르다.
// 원자료 적재(부속 fan-out)는 CompanyAggregateLoader 가 전담하고, 여기선 접근제어 + 위임 + 응답 매핑만 한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyQueryService {

    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final CompanyAggregateLoader aggregateLoader;
    private final CompanySearchRepository companySearchRepository;

    // 공개 회사 목록/검색: 통합 키워드 + 고급 + 패싯(인증/산업군/카테고리 서브트리). 색인된·비삭제 회사만,
    // 기본 정렬(spotlight→featured→최신). 엔진 교체(PG↔ES)는 CompanySearchRepository 뒤에서만 일어난다.
    public PageResponse<CompanySummary> search(CompanySearchCriteria criteria, Pageable pageable) {
        Page<CompanySummary> page = companySearchRepository.search(criteria, pageable);
        return PageResponse.of(page.getContent(), page.getTotalElements(), pageable);
    }

    // 공개(비소유자) 조회: 소프트 삭제된 회사는 미존재로 취급한다.
    public CompanyPublicResponse getPublic(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new BusinessException(CompanyErrorCode.COMPANY_NOT_FOUND));

        return CompanyPublicResponse.from(aggregateLoader.load(company));
    }

    // 소유자 전용 상세: 요청자가 해당 회사의 멤버여야 한다. 삭제된 회사도 소유자에게는 보인다.
    public CompanyDetailResponse getOwnerView(Long companyId, Long requesterId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(CompanyErrorCode.COMPANY_NOT_FOUND));

        if (!companyMemberRepository.existsByCompanyIdAndUserId(companyId, requesterId)) {
            throw new BusinessException(CompanyErrorCode.COMPANY_ACCESS_DENIED);
        }

        return CompanyDetailResponse.from(aggregateLoader.load(company));
    }

    // 관리자 상세: 상태(삭제/미연동 등) 무관하게 전체를 본다. (권한 검증은 컨트롤러 @PreAuthorize 가 담당)
    public CompanyDetailResponse getForAdmin(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(CompanyErrorCode.COMPANY_NOT_FOUND));

        return CompanyDetailResponse.from(aggregateLoader.load(company));
    }
}
