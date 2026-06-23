package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.category.CategoryRepository;
import project.plantly.domain.company.certification.CertificationRepository;
import project.plantly.domain.company.country.CountryRepository;
import project.plantly.domain.company.domesticRegion.DomesticRegionRepository;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.link.CompanyCategory;
import project.plantly.domain.company.entity.link.CompanyCertification;
import project.plantly.domain.company.entity.link.CompanyCountry;
import project.plantly.domain.company.entity.link.CompanyDomesticRegion;
import project.plantly.domain.company.entity.link.CompanyIndustry;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.industry.IndustryRepository;
import project.plantly.domain.company.repository.*;
import project.plantly.global.exception.BusinessException;

import java.util.List;
import java.util.function.BiFunction;

// 링크(M:N) 엔티티 저장 전담. 마스터 존재 검증 후 연결 엔티티를 생성·저장한다.
@Component
@RequiredArgsConstructor
public class CompanyLinkWriter {

    // 링크(M:N) 엔티티 저장소
    private final CompanyCategoryRepository companyCategoryRepository;
    private final CompanyCertificationRepository companyCertificationRepository;
    private final CompanyCountryRepository companyCountryRepository;
    private final CompanyDomesticRegionRepository companyDomesticRegionRepository;
    private final CompanyIndustryRepository companyIndustryRepository;

    // 마스터 조회소 (링크 대상 존재 검증 및 참조 확보)
    private final CategoryRepository categoryRepository;
    private final CertificationRepository certificationRepository;
    private final CountryRepository countryRepository;
    private final DomesticRegionRepository domesticRegionRepository;
    private final IndustryRepository industryRepository;

    public void write(Company company, CompanyCreateRequest request) {
        companyCategoryRepository.saveAll(
                buildLinks(request.categoryIds(), categoryRepository, company, CompanyCategory::new, CompanyErrorCode.CATEGORY_NOT_FOUND));
        companyCertificationRepository.saveAll(
                buildLinks(request.certificationIds(), certificationRepository, company, CompanyCertification::new, CompanyErrorCode.CERTIFICATION_NOT_FOUND));
        companyCountryRepository.saveAll(
                buildLinks(request.countryIds(), countryRepository, company, CompanyCountry::new, CompanyErrorCode.COUNTRY_NOT_FOUND));
        companyDomesticRegionRepository.saveAll(
                buildLinks(request.domesticRegionIds(), domesticRegionRepository, company, CompanyDomesticRegion::new, CompanyErrorCode.DOMESTIC_REGION_NOT_FOUND));
        companyIndustryRepository.saveAll(
                buildLinks(request.industryIds(), industryRepository, company, CompanyIndustry::new, CompanyErrorCode.INDUSTRY_NOT_FOUND));
    }

    // 요청 ID 리스트 → 마스터 일괄 조회(중복 제거) → 누락 시 예외 → 링크 엔티티 생성.
    // 마스터 조회를 findAllById 한 번으로 처리해 N+1 을 피한다.
    private <M, L> List<L> buildLinks(List<Long> ids,
                                      JpaRepository<M, Long> masterRepository,
                                      Company company,
                                      BiFunction<Company, M, L> linkFactory,
                                      CompanyErrorCode notFound) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Long> distinctIds = ids.stream().distinct().toList();
        List<M> masters = masterRepository.findAllById(distinctIds);

        if (masters.size() != distinctIds.size()) {
            throw new BusinessException(notFound);
        }

        return masters.stream()
                .map(master -> linkFactory.apply(company, master))
                .toList();
    }
}
