package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import project.plantly.domain.company.entity.CompanyContact;
import project.plantly.domain.company.entity.CompanyEquipment;
import project.plantly.domain.company.entity.CompanyImage;
import project.plantly.domain.company.entity.CompanyMaterial;
import project.plantly.domain.company.entity.CompanyTag;
import project.plantly.domain.company.entity.link.CompanyCategory;
import project.plantly.domain.company.entity.link.CompanyCertification;
import project.plantly.domain.company.entity.link.CompanyCountry;
import project.plantly.domain.company.entity.link.CompanyDomesticRegion;
import project.plantly.domain.company.entity.link.CompanyIndustry;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.industry.Industry;
import project.plantly.domain.company.industry.IndustryRepository;
import project.plantly.domain.company.repository.*;
import project.plantly.domain.user.enums.UserGrade;
import project.plantly.global.exception.BusinessException;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    // 자식(소유) 엔티티 저장소
    private final CompanyContactRepository contactRepository;
    private final CompanyImageRepository imageRepository;
    private final CompanyMaterialRepository materialRepository;
    private final CompanyEquipmentRepository equipmentRepository;
    private final CompanyTagRepository tagRepository;

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

    // 관리자 등록 회사는 소유자(=등급)가 없으므로 카테고리 상한은 제품 절대 상한(최상위 등급과 동일)으로 캡한다.
    private static final int ADMIN_MAX_CATEGORIES = UserGrade.PREMIUM.getMaxCompanyCategories();

    // 유저 자가등록: 등록 즉시 소유자 = 본인. 카테고리 상한은 본인 등급을 따른다.
    @Transactional
    public Long createByUser(Long userId, UserGrade grade, CompanyCreateRequest request) {
        Company company = Company.createByUser(
                userId,
                request.businessNumber(), request.companyName(), request.ceoName(), request.establishmentDate(),
                request.postalCode(), request.address(), request.detailAddress(), request.website(), request.logoUrl(),
                request.introTitle(), request.content(), request.trlLevel(), request.projectTitle(), request.achievements(),
                request.partners(), request.videoUrl(), request.leadTime(), request.asInfo(), request.pricingType(), request.brandColor());

        return persist(company, request, grade.getMaxCompanyCategories());
    }

    // 관리자 등록: 소유자 미연동(userId=null) 상태로 시작. registeredBy = 등록한 admin id.
    @Transactional
    public Long createByAdmin(Long adminId, CompanyCreateRequest request) {
        Company company = Company.createByAdmin(
                adminId,
                request.businessNumber(), request.companyName(), request.ceoName(), request.establishmentDate(),
                request.postalCode(), request.address(), request.detailAddress(), request.website(), request.logoUrl(),
                request.introTitle(), request.content(), request.trlLevel(), request.projectTitle(), request.achievements(),
                request.partners(), request.videoUrl(), request.leadTime(), request.asInfo(), request.pricingType(), request.brandColor());

        return persist(company, request, ADMIN_MAX_CATEGORIES);
    }

    // 공통 코어: 등급별 카테고리 상한 검증 후, 본체 INSERT(=id 확보) → 부속 10종을 같은 트랜잭션으로 저장한다.
    private Long persist(Company company, CompanyCreateRequest request, int maxCategories) {
        validateCategoryLimit(request.categoryIds(), maxCategories);

        companyRepository.save(company);
        saveChildren(company, request);
        saveLinks(company, request);
        return company.getId();
    }

    // 실제 저장될 카테고리(중복 제거 후) 개수가 등급 상한을 넘으면 거부한다.
    private void validateCategoryLimit(List<Long> categoryIds, int maxCategories) {
        if (categoryIds == null) {
            return;
        }

        long distinctCount = categoryIds.stream().distinct().count();
        if (distinctCount > maxCategories) {
            throw new BusinessException(CompanyErrorCode.CATEGORY_LIMIT_EXCEEDED);
        }
    }

    // ===== 자식(소유) 엔티티 — 요청 값을 그대로 저장. displayOrder 는 리스트 인덱스로 부여 =====
    private void saveChildren(Company company, CompanyCreateRequest request) {
        if (request.contacts() != null) {
            List<CompanyContact> contacts = request.contacts().stream()
                    .map(c -> new CompanyContact(company, c.contactName(), c.position(), c.phone(), c.email()))
                    .toList();
            contactRepository.saveAll(contacts);
        }

        if (request.images() != null) {
            List<CompanyImage> images = IntStream.range(0, request.images().size())
                    .mapToObj(i -> {
                        CompanyCreateRequest.ImageRequest img = request.images().get(i);
                        return new CompanyImage(company, img.imageUrl(), img.imageType(), i);
                    })
                    .toList();
            imageRepository.saveAll(images);
        }

        if (request.materialNames() != null) {
            List<CompanyMaterial> materials = IntStream.range(0, request.materialNames().size())
                    .mapToObj(i -> new CompanyMaterial(company, request.materialNames().get(i), i))
                    .toList();
            materialRepository.saveAll(materials);
        }

        if (request.equipmentNames() != null) {
            List<CompanyEquipment> equipments = IntStream.range(0, request.equipmentNames().size())
                    .mapToObj(i -> new CompanyEquipment(company, request.equipmentNames().get(i), i))
                    .toList();
            equipmentRepository.saveAll(equipments);
        }

        if (request.tagNames() != null) {
            List<CompanyTag> tags = request.tagNames().stream()
                    .map(t -> new CompanyTag(company, t))
                    .toList();
            tagRepository.saveAll(tags);
        }
    }

    // ===== 링크(M:N) 엔티티 — 마스터 존재 검증 후 연결 저장 =====
    private void saveLinks(Company company, CompanyCreateRequest request) {
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
