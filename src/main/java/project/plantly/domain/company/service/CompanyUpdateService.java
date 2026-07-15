package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.dto.AdminSubscriptionUpdateRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.dto.CompanyUpdateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.enums.CompanyVisibility;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.policy.CompanyMutationPolicy;
import project.plantly.domain.company.policy.CompanyPolicyView;
import project.plantly.domain.company.repository.CompanyMemberRepository;
import project.plantly.domain.company.repository.CompanyRepository;
import project.plantly.domain.company.repository.CompanySubscriptionRepository;
import project.plantly.domain.company.search.CompanySearchDocumentWriter;
import project.plantly.global.exception.BusinessException;

import java.util.List;
import java.util.function.BiFunction;
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
    private final CompanySubscriptionRepository companySubscriptionRepository;
    private final CompanyChildWriter childWriter;
    private final CompanyLinkWriter linkWriter;
    private final CompanySearchDocumentWriter searchDocumentWriter;

    // 등록·수정 공통 등급 정책 중 '수정에도 재실행돼야 하는' 것들(CompanyMutationPolicy). 등급 한도 우회를 막는다.
    // create 는 전 정책(List<CompanyRegistrationPolicy>)을, 수정은 이 부분집합만 실행한다. Spotlight/GalleryImageType 은 자연 제외.
    private final List<CompanyMutationPolicy> mutationPolicies;

    // ===== 기본 정보 부분 수정 =====

    public void updateBasicInfoByUser(Long companyId, Long userId, CompanyUpdateRequest request) {
        mutateOwnedGraded(companyId, userId,
                company -> company.updateBasicInfo(
                        request.companyName(), request.ceoName(), request.establishmentDate(),
                        request.postalCode(), request.address(), request.detailAddress(),
                        request.website(), request.logoUrl(), request.introTitle(), request.content(),
                        request.trlLevel(), request.videoUrl(), request.leadTime(), request.asInfo(),
                        request.pricingType(), request.brandColor()),
                (company, sub) -> CompanyPolicyView.forBasicInfoUpdate(company, sub, request.videoUrl(), request.brandColor() != null));
    }

    // ===== 공개/비공개 전환 =====
    // 소유자 경로는 멤버 검증 후, 관리자 경로는 소유 무관으로 목표 상태를 지정한다(멱등). 둘 다 공통 실행 골격을
    // 재사용한다 — visibility 는 검색 도큐먼트에 없어 재색인이 불필요하지만(공개 필터는 company.visibility 를 실시간 참조),
    // 경로를 하나로 유지하려고 mutateOwned/mutateAsAdmin 을 그대로 태운다(도큐먼트 재생성은 멱등).

    public void changeVisibilityByUser(Long companyId, Long userId, CompanyVisibility visibility) {
        mutateOwned(companyId, userId, company -> company.changeVisibility(visibility));
    }

    public void changeVisibilityByAdmin(Long companyId, CompanyVisibility visibility) {
        mutateAsAdmin(companyId, company -> company.changeVisibility(visibility));
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
        mutateOwnedGraded(companyId, userId,
                company -> childWriter.replaceGalleryImages(company, images),
                (company, sub) -> CompanyPolicyView.forGalleryUpdate(company, sub, images));
    }

    public void replaceContactsByUser(Long companyId, Long userId, List<CompanyCreateRequest.ContactRequest> contacts) {
        mutateOwned(companyId, userId, company -> childWriter.replaceContacts(company, contacts));
    }

    public void replaceReferencesByUser(Long companyId, Long userId, List<CompanyCreateRequest.ReferenceRequest> references) {
        mutateOwnedGraded(companyId, userId,
                company -> childWriter.replaceReferences(company, references),
                (company, sub) -> CompanyPolicyView.forReferenceUpdate(company, sub, references));
    }

    public void replaceCategoriesByUser(Long companyId, Long userId, List<Long> categoryIds) {
        mutateOwnedGraded(companyId, userId,
                company -> linkWriter.replaceCategories(company, categoryIds),
                (company, sub) -> CompanyPolicyView.forCategoryUpdate(company, sub, categoryIds));
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

    // ===== 관리자 경로 =====
    // 소유(멤버) 검증 없이 대상 회사를 로드한다(삭제/미연동 포함). 권한(ADMIN)은 컨트롤러 @PreAuthorize 가 담당한다.
    // 변경 로직·구조 불변식은 유저 경로와 완전히 동일하게 재사용한다.

    public void updateBasicInfoByAdmin(Long companyId, CompanyUpdateRequest request) {
        mutateAsAdminGraded(companyId,
                company -> company.updateBasicInfo(
                        request.companyName(), request.ceoName(), request.establishmentDate(),
                        request.postalCode(), request.address(), request.detailAddress(),
                        request.website(), request.logoUrl(), request.introTitle(), request.content(),
                        request.trlLevel(), request.videoUrl(), request.leadTime(), request.asInfo(),
                        request.pricingType(), request.brandColor()),
                (company, sub) -> CompanyPolicyView.forBasicInfoUpdate(company, sub, request.videoUrl(), request.brandColor() != null));
    }

    public void replaceTagsByAdmin(Long companyId, List<String> tagNames) {
        mutateAsAdmin(companyId, company -> childWriter.replaceTags(company, tagNames));
    }

    public void replaceMaterialsByAdmin(Long companyId, List<String> materialNames) {
        mutateAsAdmin(companyId, company -> childWriter.replaceMaterials(company, materialNames));
    }

    public void replaceEquipmentByAdmin(Long companyId, List<String> equipmentNames) {
        mutateAsAdmin(companyId, company -> childWriter.replaceEquipment(company, equipmentNames));
    }

    public void replaceGalleryImagesByAdmin(Long companyId, List<CompanyCreateRequest.ImageRequest> images) {
        mutateAsAdminGraded(companyId,
                company -> childWriter.replaceGalleryImages(company, images),
                (company, sub) -> CompanyPolicyView.forGalleryUpdate(company, sub, images));
    }

    public void replaceContactsByAdmin(Long companyId, List<CompanyCreateRequest.ContactRequest> contacts) {
        mutateAsAdmin(companyId, company -> childWriter.replaceContacts(company, contacts));
    }

    public void replaceReferencesByAdmin(Long companyId, List<CompanyCreateRequest.ReferenceRequest> references) {
        mutateAsAdminGraded(companyId,
                company -> childWriter.replaceReferences(company, references),
                (company, sub) -> CompanyPolicyView.forReferenceUpdate(company, sub, references));
    }

    public void replaceCategoriesByAdmin(Long companyId, List<Long> categoryIds) {
        mutateAsAdminGraded(companyId,
                company -> linkWriter.replaceCategories(company, categoryIds),
                (company, sub) -> CompanyPolicyView.forCategoryUpdate(company, sub, categoryIds));
    }

    public void replaceIndustriesByAdmin(Long companyId, List<Long> industryIds) {
        mutateAsAdmin(companyId, company -> linkWriter.replaceIndustries(company, industryIds));
    }

    public void replaceCertificationsByAdmin(Long companyId, List<Long> certificationIds) {
        mutateAsAdmin(companyId, company -> linkWriter.replaceCertifications(company, certificationIds));
    }

    public void replaceCountriesByAdmin(Long companyId, List<Long> countryIds) {
        mutateAsAdmin(companyId, company -> linkWriter.replaceCountries(company, countryIds));
    }

    public void replaceRegionsByAdmin(Long companyId, List<Long> domesticRegionIds) {
        mutateAsAdmin(companyId, company -> linkWriter.replaceRegions(company, domesticRegionIds));
    }

    // ===== 관리자 구독 수정 (raw full-replace) =====
    // 관리자가 grade/status/expiresAt 를 직접 지정한다(팝업의 라인별 드롭다운). 회사 컬렉션 변경이 아니라
    // 구독 테이블만 건드리므로 등급 정책 재실행·검색 재동기화가 필요 없다(검색 도큐먼트에 grade 없음).
    // 다운그레이드로 초과된 기존 데이터(카테고리 수 등)를 소급해서 잘라내지도 않는다 — 그 컬렉션을 수정하는 시점에
    // 정책이 자연히 적용된다(기존 결정과 일관). effectiveGrade 는 파생이라 저장 갱신이 없다.
    public void updateSubscriptionByAdmin(Long companyId, AdminSubscriptionUpdateRequest request) {
        CompanySubscription subscription = companySubscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new BusinessException(CompanyErrorCode.COMPANY_NOT_FOUND));

        subscription.changeByAdmin(request.grade(), request.status(), request.expiresAt());
    }

    // ===== 공통 실행 골격 =====

    // 소유(멤버) 검증 → 변경 적용 → 검색 도큐먼트 재동기화. 등급 정책이 없는 컬렉션(태그·소재·장비·연락처·산업군 등)이 쓴다.
    // (검색 색인 대상이 아닌 컬렉션까지 매번 재동기화하지만, 도큐먼트 재생성은 멱등이라 정합성 우선으로 일괄 호출한다)
    private void mutateOwned(Long companyId, Long userId, Consumer<Company> mutation) {
        Company company = loadOwnedCompany(companyId, userId);
        mutation.accept(company);
        searchDocumentWriter.write(companyId);
    }

    // 관리자 경로: 소유 검증 없이 로드 → 변경 → 재동기화. (getForAdmin 과 동일하게 상태 무관 로드)
    private void mutateAsAdmin(Long companyId, Consumer<Company> mutation) {
        Company company = loadCompany(companyId);
        mutation.accept(company);
        searchDocumentWriter.write(companyId);
    }

    // ===== 등급 정책 재실행 골격 (등급 한도가 걸리는 수정 경로 전용) =====
    // 소유 검증 → 변경 적용 → '이번에 바뀐 부분'만 담은 delta 뷰로 등급 정책(CompanyMutationPolicy) 재실행 → 재동기화.
    // 정책이 던지면 트랜잭션이 롤백되어 아무것도 반영되지 않는다(수정으로 등급 한도를 우회하는 구멍을 닫는다).
    // 한도는 '행위자'가 아니라 '회사의 구독'이 정한다(유저/관리자 동일). ADMIN_EXEMPT 회사는 각 정책이 자연히 면제한다.
    private void mutateOwnedGraded(Long companyId, Long userId, Consumer<Company> mutation,
                                   BiFunction<Company, CompanySubscription, CompanyPolicyView> viewFactory) {
        Company company = loadOwnedCompany(companyId, userId);
        applyGradedMutation(companyId, company, mutation, viewFactory);
    }

    private void mutateAsAdminGraded(Long companyId, Consumer<Company> mutation,
                                     BiFunction<Company, CompanySubscription, CompanyPolicyView> viewFactory) {
        Company company = loadCompany(companyId);
        applyGradedMutation(companyId, company, mutation, viewFactory);
    }

    private void applyGradedMutation(Long companyId, Company company, Consumer<Company> mutation,
                                     BiFunction<Company, CompanySubscription, CompanyPolicyView> viewFactory) {
        CompanySubscription subscription = loadSubscription(companyId);
        mutation.accept(company);
        CompanyPolicyView view = viewFactory.apply(company, subscription);
        mutationPolicies.forEach(policy -> policy.apply(view));
        searchDocumentWriter.write(companyId);
    }

    // 소유(멤버) 검증까지 통과한 회사를 로드한다. (getOwnerView 와 동일 정책)
    private Company loadOwnedCompany(Long companyId, Long userId) {
        Company company = loadCompany(companyId);
        if (!companyMemberRepository.existsByCompanyIdAndUserId(companyId, userId)) {
            throw new BusinessException(CompanyErrorCode.COMPANY_ACCESS_DENIED);
        }
        return company;
    }

    private Company loadCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(CompanyErrorCode.COMPANY_NOT_FOUND));
    }

    // 등급 정책이 참조할 회사 구독(1:1). 모든 회사는 등록 시 구독을 1건 갖는 불변식이라, 없으면 데이터 정합성 오류다.
    private CompanySubscription loadSubscription(Long companyId) {
        return companySubscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new IllegalStateException("회사 구독이 존재하지 않습니다(불변식 위반): companyId=" + companyId));
    }
}
