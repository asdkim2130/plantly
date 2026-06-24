package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.dto.CompanyAggregate;
import project.plantly.domain.company.dto.CompanyDetailResponse;
import project.plantly.domain.company.dto.CompanyPublicResponse;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanyImage;
import project.plantly.domain.company.entity.CompanyProjectReference;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.repository.*;
import project.plantly.global.exception.BusinessException;

import java.util.List;

// 회사 상세 조회 전담 서비스. 등록(CompanyService)과 분리한 읽기 전용 경로.
// 세 진입점(공개 / 소유자 / 관리자)은 '누가 무엇을 볼 수 있는가'(접근 제어 + 응답 형태)만 다르고,
// 원자료 적재(loadAggregate)는 공유한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyQueryService {

    private final CompanyRepository companyRepository;

    private final CompanyContactRepository contactRepository;
    private final CompanyImageRepository imageRepository;
    private final CompanyMaterialRepository materialRepository;
    private final CompanyEquipmentRepository equipmentRepository;
    private final CompanyTagRepository tagRepository;
    private final CompanyProjectReferenceRepository referenceRepository;

    private final CompanyCategoryRepository categoryRepository;
    private final CompanyCertificationRepository certificationRepository;
    private final CompanyCountryRepository countryRepository;
    private final CompanyDomesticRegionRepository domesticRegionRepository;
    private final CompanyIndustryRepository industryRepository;

    private final CompanyMemberRepository companyMemberRepository;

    // 공개(비소유자) 조회: 소프트 삭제된 회사는 미존재로 취급한다.
    public CompanyPublicResponse getPublic(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new BusinessException(CompanyErrorCode.COMPANY_NOT_FOUND));

        return CompanyPublicResponse.from(loadAggregate(company));
    }

    // 소유자 전용 상세: 요청자가 해당 회사의 멤버여야 한다. 삭제된 회사도 소유자에게는 보인다.
    public CompanyDetailResponse getOwnerView(Long companyId, Long requesterId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(CompanyErrorCode.COMPANY_NOT_FOUND));

        if (!companyMemberRepository.existsByCompanyIdAndUserId(companyId, requesterId)) {
            throw new BusinessException(CompanyErrorCode.COMPANY_ACCESS_DENIED);
        }

        return CompanyDetailResponse.from(loadAggregate(company));
    }

    // 관리자 상세: 상태(삭제/미연동 등) 무관하게 전체를 본다. (권한 검증은 컨트롤러 @PreAuthorize 가 담당)
    public CompanyDetailResponse getForAdmin(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(CompanyErrorCode.COMPANY_NOT_FOUND));

        return CompanyDetailResponse.from(loadAggregate(company));
    }

    // 본체 + 부속(자식/링크)을 적재해 응답 형태에 독립적인 원자료 묶음을 만든다.
    private CompanyAggregate loadAggregate(Company company) {
        Long companyId = company.getId();

        // 연락처/레퍼런스는 대표 1건만 상세에 싣는다. (전체 목록은 추후 '더보기' 전용 조회로 분리)
        CompanyProjectReference representativeReference = referenceRepository
                .findFirstByCompanyIdAndRepresentativeTrueOrderByDisplayOrderAsc(companyId)
                .orElse(null);
        Long representativeReferenceId = representativeReference == null ? null : representativeReference.getId();

        // 이미지는 한 번에 가져와 갤러리(projectReference == null)와 '대표 레퍼런스에 딸린 이미지'로 분리한다.
        List<CompanyImage> allImages = imageRepository.findByCompanyIdOrderByDisplayOrderAsc(companyId);
        List<CompanyImage> galleryImages = allImages.stream()
                .filter(image -> image.getProjectReference() == null)
                .toList();
        List<CompanyImage> representativeReferenceImages = representativeReferenceId == null ? List.of()
                : allImages.stream()
                        .filter(image -> image.getProjectReference() != null
                                && representativeReferenceId.equals(image.getProjectReference().getId()))
                        .toList();

        return new CompanyAggregate(
                company,
                contactRepository.findFirstByCompanyIdAndRepresentativeTrueOrderByDisplayOrderAsc(companyId)
                        .orElse(null),
                galleryImages,
                representativeReference,
                representativeReferenceImages,
                materialRepository.findByCompanyIdOrderByDisplayOrderAsc(companyId),
                equipmentRepository.findByCompanyIdOrderByDisplayOrderAsc(companyId),
                tagRepository.findByCompanyIdOrderByDisplayOrderAsc(companyId),
                categoryRepository.findCategoriesByCompanyId(companyId),
                certificationRepository.findCertificationsByCompanyId(companyId),
                countryRepository.findCountriesByCompanyId(companyId),
                domesticRegionRepository.findRegionsByCompanyId(companyId),
                industryRepository.findIndustriesByCompanyId(companyId));
    }
}
