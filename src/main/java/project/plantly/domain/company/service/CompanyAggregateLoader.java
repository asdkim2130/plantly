package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.dto.CompanyAggregate;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanyImage;
import project.plantly.domain.company.entity.CompanyProjectReference;
import project.plantly.domain.company.repository.*;

import java.util.List;

// 회사 본체 + 부속(자식/링크)을 적재해 응답 형태에 독립적인 원자료(CompanyAggregate)로 조립하는 읽기 협력자.
// CompanyQueryService 가 접근제어를 끝낸 Company 를 넘기면, 여기서 부속 repository fan-out 과 가공을 전담한다.
// (조회 서비스의 오케스트레이션과 '적재' 책임을 분리 — 부속 repository 의존은 모두 이 클래스가 소유한다)
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
class CompanyAggregateLoader {

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

    // 본체 + 부속(자식/링크)을 적재해 응답 형태에 독립적인 원자료 묶음을 만든다.
    CompanyAggregate load(Company company) {
        Long companyId = company.getId();

        // 연락처/레퍼런스는 대표 1건만 상세에 싣는다. (전체 목록은 추후 '더보기' 전용 조회로 분리)
        CompanyProjectReference representativeReference = referenceRepository
                .findFirstByCompanyIdAndRepresentativeTrueOrderByDisplayOrderAsc(companyId)
                .orElse(null);
        Long representativeReferenceId = representativeReference == null ? null : representativeReference.getId();

        // 이미지는 한 번에 가져와 갤러리(projectReference == null)와 '대표 레퍼런스 표지 1장'으로 나눈다.
        // allImages 가 displayOrder 오름차순이라, 대표 레퍼런스의 첫 매칭이 곧 표지(썸네일)다.
        // (전체 이미지는 상세에 싣지 않는다 — 추후 '레퍼런스 더보기' 전용 조회로 분리)
        List<CompanyImage> allImages = imageRepository.findByCompanyIdOrderByDisplayOrderAsc(companyId);
        List<CompanyImage> galleryImages = allImages.stream()
                .filter(image -> image.getProjectReference() == null)
                .toList();
        CompanyImage representativeReferenceThumbnail = representativeReferenceId == null ? null
                : allImages.stream()
                        .filter(image -> image.getProjectReference() != null
                                && representativeReferenceId.equals(image.getProjectReference().getId()))
                        .findFirst()
                        .orElse(null);

        return new CompanyAggregate(
                company,
                contactRepository.findFirstByCompanyIdAndRepresentativeTrueOrderByDisplayOrderAsc(companyId)
                        .orElse(null),
                galleryImages,
                representativeReference,
                representativeReferenceThumbnail,
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
