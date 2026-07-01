package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.category.Category;
import project.plantly.domain.company.category.CategoryRepository;
import project.plantly.domain.company.certification.Certification;
import project.plantly.domain.company.certification.CertificationRepository;
import project.plantly.domain.company.country.Country;
import project.plantly.domain.company.country.CountryRepository;
import project.plantly.domain.company.domesticRegion.DomesticRegion;
import project.plantly.domain.company.domesticRegion.DomesticRegionRepository;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.link.CompanyCategory;
import project.plantly.domain.company.entity.link.CompanyCertification;
import project.plantly.domain.company.entity.link.CompanyCountry;
import project.plantly.domain.company.entity.link.CompanyDomesticRegion;
import project.plantly.domain.company.entity.link.CompanyIndustry;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.industry.Industry;
import project.plantly.domain.company.industry.IndustryRepository;
import project.plantly.domain.company.repository.*;
import project.plantly.global.exception.BusinessException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// 링크(M:N) 엔티티 저장 전담. 마스터 존재 검증 후 연결 엔티티를 생성·저장한다.
// 각 링크의 displayOrder 는 회사가 등록 시 보낸 요청(선택) 순서로 부여한다.
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
                buildLinks(request.categoryIds(), categoryRepository, Category::getId, company, CompanyCategory::new, CompanyErrorCode.CATEGORY_NOT_FOUND));
        companyCertificationRepository.saveAll(
                buildLinks(request.certificationIds(), certificationRepository, Certification::getId, company, CompanyCertification::new, CompanyErrorCode.CERTIFICATION_NOT_FOUND));
        companyCountryRepository.saveAll(
                buildLinks(request.countryIds(), countryRepository, Country::getId, company, CompanyCountry::new, CompanyErrorCode.COUNTRY_NOT_FOUND));
        companyDomesticRegionRepository.saveAll(
                buildLinks(request.domesticRegionIds(), domesticRegionRepository, DomesticRegion::getId, company, CompanyDomesticRegion::new, CompanyErrorCode.DOMESTIC_REGION_NOT_FOUND));
        companyIndustryRepository.saveAll(
                buildLinks(request.industryIds(), industryRepository, Industry::getId, company, CompanyIndustry::new, CompanyErrorCode.INDUSTRY_NOT_FOUND));
    }

    // ===== 링크 전체 교체(PUT) 진입점 =====
    // 각 메서드는 "기존 링크 삭제 → 새 id 리스트로 재생성(displayOrder 재부여)" 하며, create 경로와 검증·저장 로직을 공유한다.
    // null/빈 리스트를 넘기면 삭제만 수행(= 전부 비우기)된다.

    public void replaceCategories(Company company, List<Long> categoryIds) {
        companyCategoryRepository.deleteByCompanyId(company.getId());
        companyCategoryRepository.saveAll(
                buildLinks(categoryIds, categoryRepository, Category::getId, company, CompanyCategory::new, CompanyErrorCode.CATEGORY_NOT_FOUND));
    }

    public void replaceCertifications(Company company, List<Long> certificationIds) {
        companyCertificationRepository.deleteByCompanyId(company.getId());
        companyCertificationRepository.saveAll(
                buildLinks(certificationIds, certificationRepository, Certification::getId, company, CompanyCertification::new, CompanyErrorCode.CERTIFICATION_NOT_FOUND));
    }

    public void replaceCountries(Company company, List<Long> countryIds) {
        companyCountryRepository.deleteByCompanyId(company.getId());
        companyCountryRepository.saveAll(
                buildLinks(countryIds, countryRepository, Country::getId, company, CompanyCountry::new, CompanyErrorCode.COUNTRY_NOT_FOUND));
    }

    public void replaceRegions(Company company, List<Long> domesticRegionIds) {
        companyDomesticRegionRepository.deleteByCompanyId(company.getId());
        companyDomesticRegionRepository.saveAll(
                buildLinks(domesticRegionIds, domesticRegionRepository, DomesticRegion::getId, company, CompanyDomesticRegion::new, CompanyErrorCode.DOMESTIC_REGION_NOT_FOUND));
    }

    public void replaceIndustries(Company company, List<Long> industryIds) {
        companyIndustryRepository.deleteByCompanyId(company.getId());
        companyIndustryRepository.saveAll(
                buildLinks(industryIds, industryRepository, Industry::getId, company, CompanyIndustry::new, CompanyErrorCode.INDUSTRY_NOT_FOUND));
    }

    // 요청 ID 리스트 → 마스터 일괄 조회(중복 제거) → 누락 시 예외 → 링크 엔티티 생성.
    // findAllById 는 입력 순서를 보존하지 않으므로, displayOrder 는 요청(선택) 순서 기준 인덱스로 부여한다.
    private <M, L> List<L> buildLinks(List<Long> ids,
                                      JpaRepository<M, Long> masterRepository,
                                      Function<M, Long> idExtractor,
                                      Company company,
                                      LinkFactory<M, L> linkFactory,
                                      CompanyErrorCode notFound) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Long> distinctIds = ids.stream().distinct().toList(); // 요청(선택) 순서 보존
        List<M> masters = masterRepository.findAllById(distinctIds);

        if (masters.size() != distinctIds.size()) {
            throw new BusinessException(notFound);
        }

        Map<Long, M> mastersById = masters.stream().collect(Collectors.toMap(idExtractor, Function.identity()));

        return IntStream.range(0, distinctIds.size())
                .mapToObj(i -> linkFactory.create(company, mastersById.get(distinctIds.get(i)), i))
                .toList();
    }

    // 링크 엔티티 생성 팩토리. (회사, 마스터, 선택 순서) → 링크 엔티티.
    @FunctionalInterface
    private interface LinkFactory<M, L> {
        L create(Company company, M master, int displayOrder);
    }
}
