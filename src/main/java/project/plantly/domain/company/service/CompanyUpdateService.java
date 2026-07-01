package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.dto.CompanyUpdateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.repository.CompanyMemberRepository;
import project.plantly.domain.company.repository.CompanyRepository;
import project.plantly.domain.company.search.CompanySearchDocumentWriter;
import project.plantly.global.exception.BusinessException;

import java.util.List;
import java.util.function.Consumer;

// 회사 수정 전담 서비스. 등록(CompanyService)·조회(CompanyQueryService)와 분리한 변경 경로.
// 본체 기본 정보는 부분 수정(PATCH), 컬렉션은 전체 교체(PUT)로 다룬다.
// 어떤 수정이든 mutateOwned 를 거쳐 (소유 검증 → 변경 → 검색 재동기화) 순서를 강제해 비정규화 색인을 최신화한다.
// 접근제어: 유저 경로는 소유(멤버)여야 하고, 관리자 경로는 컨트롤러 @PreAuthorize 가 담당한다(추후 추가).
@Service
@RequiredArgsConstructor
@Transactional
public class CompanyUpdateService {

    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final CompanyChildWriter childWriter;
    private final CompanyLinkWriter linkWriter;
    private final CompanySearchDocumentWriter searchDocumentWriter;

    // ===== 기본 정보 부분 수정 =====

    public void updateBasicInfoByUser(Long companyId, Long userId, CompanyUpdateRequest request) {
        mutateOwned(companyId, userId, company -> company.updateBasicInfo(
                request.companyName(), request.ceoName(), request.establishmentDate(),
                request.postalCode(), request.address(), request.detailAddress(),
                request.website(), request.logoUrl(), request.introTitle(), request.content(),
                request.trlLevel(), request.videoUrl(), request.leadTime(), request.asInfo(),
                request.pricingType(), request.brandColor()));
    }

    // ===== 컬렉션 전체 교체 =====
    // 각 메서드는 해당 컬렉션만 통째로 새 리스트로 교체한다(빈 리스트 = 전부 비우기).

    public void replaceTagsByUser(Long companyId, Long userId, List<String> tagNames) {
        mutateOwned(companyId, userId, company -> childWriter.replaceTags(company, tagNames));
    }

    public void replaceMaterialsByUser(Long companyId, Long userId, List<String> materialNames) {
        mutateOwned(companyId, userId, company -> childWriter.replaceMaterials(company, materialNames));
    }

    public void replaceEquipmentByUser(Long companyId, Long userId, List<String> equipmentNames) {
        mutateOwned(companyId, userId, company -> childWriter.replaceEquipment(company, equipmentNames));
    }

    public void replaceGalleryImagesByUser(Long companyId, Long userId, List<CompanyCreateRequest.ImageRequest> images) {
        mutateOwned(companyId, userId, company -> childWriter.replaceGalleryImages(company, images));
    }

    public void replaceContactsByUser(Long companyId, Long userId, List<CompanyCreateRequest.ContactRequest> contacts) {
        mutateOwned(companyId, userId, company -> childWriter.replaceContacts(company, contacts));
    }

    public void replaceReferencesByUser(Long companyId, Long userId, List<CompanyCreateRequest.ReferenceRequest> references) {
        mutateOwned(companyId, userId, company -> childWriter.replaceReferences(company, references));
    }

    public void replaceCategoriesByUser(Long companyId, Long userId, List<Long> categoryIds) {
        mutateOwned(companyId, userId, company -> linkWriter.replaceCategories(company, categoryIds));
    }

    public void replaceIndustriesByUser(Long companyId, Long userId, List<Long> industryIds) {
        mutateOwned(companyId, userId, company -> linkWriter.replaceIndustries(company, industryIds));
    }

    public void replaceCertificationsByUser(Long companyId, Long userId, List<Long> certificationIds) {
        mutateOwned(companyId, userId, company -> linkWriter.replaceCertifications(company, certificationIds));
    }

    public void replaceCountriesByUser(Long companyId, Long userId, List<Long> countryIds) {
        mutateOwned(companyId, userId, company -> linkWriter.replaceCountries(company, countryIds));
    }

    public void replaceRegionsByUser(Long companyId, Long userId, List<Long> domesticRegionIds) {
        mutateOwned(companyId, userId, company -> linkWriter.replaceRegions(company, domesticRegionIds));
    }

    // TODO(정책): 현재 수정 경로는 '구조 불변식'(갤러리 DETAIL-only·연락처/레퍼런스 1건)만 강제한다.
    //  등급 한도(카테고리 개수·갤러리 장수·동영상·레퍼런스 이미지)와 변형 정책(brandColor 고정·spotlight)은
    //  create 의 registrationPolicies 에만 있고 여기선 미적용 → 수정으로 우회 가능.
    //  '등급 → Company 이전' 완료 후, CompanyRegistrationPolicy 를 검증/변형으로 분리해 검증 정책만 재실행하도록 확장한다.

    // ===== 공통 실행 골격 =====

    // 소유(멤버) 검증 → 변경 적용 → 검색 도큐먼트 재동기화. 모든 수정 경로가 이 순서를 공유한다.
    // (검색 색인 대상이 아닌 컬렉션까지 매번 재동기화하지만, 도큐먼트 재생성은 멱등이라 정합성 우선으로 일괄 호출한다)
    private void mutateOwned(Long companyId, Long userId, Consumer<Company> mutation) {
        Company company = loadOwnedCompany(companyId, userId);
        mutation.accept(company);
        searchDocumentWriter.write(companyId);
    }

    // 소유(멤버) 검증까지 통과한 회사를 로드한다. (getOwnerView 와 동일 정책)
    private Company loadOwnedCompany(Long companyId, Long userId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(CompanyErrorCode.COMPANY_NOT_FOUND));
        if (!companyMemberRepository.existsByCompanyIdAndUserId(companyId, userId)) {
            throw new BusinessException(CompanyErrorCode.COMPANY_ACCESS_DENIED);
        }
        return company;
    }
}
