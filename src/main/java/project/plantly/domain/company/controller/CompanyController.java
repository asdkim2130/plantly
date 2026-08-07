package project.plantly.domain.company.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.dto.CompanyCreateRequest.ContactRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ImageRequest;
import project.plantly.domain.company.dto.CompanyCreateRequest.ReferenceRequest;
import project.plantly.domain.company.dto.CompanyDetailResponse;
import project.plantly.domain.company.dto.CompanyDraftResponse;
import project.plantly.domain.company.dto.CompanyPublicResponse;
import project.plantly.domain.company.dto.CompanyShowcaseResponse;
import project.plantly.domain.company.dto.CompanyReverificationRequest;
import project.plantly.domain.company.dto.CompanyReverificationResponse;
import project.plantly.domain.company.dto.CompanySubscriptionResponse;
import project.plantly.domain.company.dto.CompanyUpdateRequest;
import project.plantly.domain.company.dto.CompanyVerificationRequest;
import project.plantly.domain.company.dto.CompanyVerificationResponse;
import project.plantly.domain.company.dto.CompanyVisibilityUpdateRequest;
import project.plantly.domain.company.dto.MyCompanyCreateRequest;
import project.plantly.domain.company.search.dto.CompanySearchRequest;
import project.plantly.domain.company.search.dto.CompanySummary;
import project.plantly.domain.company.service.CompanyDraftService;
import project.plantly.domain.company.service.CompanyQueryService;
import project.plantly.domain.company.service.CompanyService;
import project.plantly.domain.company.service.CompanyUpdateService;
import project.plantly.domain.company.service.CompanyVerificationService;
import project.plantly.global.PageResponse;
import project.plantly.global.response.ApiResponse;
import project.plantly.global.response.IdResponse;
import project.plantly.global.security.UserPrincipal;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyQueryService companyQueryService;
    private final CompanyUpdateService companyUpdateService;
    private final CompanyVerificationService companyVerificationService;
    private final CompanyDraftService companyDraftService;

    // 사업자 인증 — 회사 등록 폼 앞단에서 아이디 중복 확인처럼 먼저 수행한다.
    // 국세청 진위확인(사업자번호+대표자명+개업일자) + 상태조회(계속사업자 여부) + 중복 검사를 한 번에 통과해야
    // verificationId 를 발급한다. 이 식별자를 등록 요청에 실어 보내면 서버가 검증된 값을 채운다.
    // 등록보다 먼저 두는 이유: 회사 정보를 20여 개 채운 뒤 마지막에 사업자번호 중복으로 튕기지 않게 하기 위해.
    @PostMapping("/api/v1/companies/verification")
    public ApiResponse<CompanyVerificationResponse> verifyBusiness(@AuthenticationPrincipal UserPrincipal principal,
                                                                    @Valid @RequestBody CompanyVerificationRequest request) {
        CompanyVerificationResponse response = companyVerificationService.verify(principal.getUser().getId(), request);
        return ApiResponse.success("사업자 인증이 완료되었습니다.", response);
    }

    // 사업자 재인증 — 소유자만. 사업자번호는 요청으로 받지 않고(DB 저장값 사용) 새 대표자명·개업일자만 국세청에
    // 재확인한다. 통과하면 두 값을 검증값으로 덮어쓰고 인증 시각을 새로 찍는다. 국세청 인증을 받은 회사는 일반
    // 수정(PATCH /{id})으로 대표자명·개업일자를 바꿀 수 없으므로, 국세청 정보가 바뀌었을 때 이 경로로만 갱신한다.
    // (최초 인증 후 1년 경과 시 재인증을 유도하는 이벤트의 실행 창구가 될 자리 — 이벤트 발행 자체는 추후.)
    @PostMapping("/api/v1/companies/{id}/verification")
    public ApiResponse<CompanyReverificationResponse> reverifyBusiness(@AuthenticationPrincipal UserPrincipal principal,
                                                                       @PathVariable Long id,
                                                                       @Valid @RequestBody CompanyReverificationRequest request) {
        CompanyReverificationResponse response =
                companyVerificationService.reverify(principal.getUser().getId(), id, request);
        return ApiResponse.success("사업자 재인증이 완료되었습니다.", response);
    }

    // 유저 자가등록 — 인증된 본인이 소유자가 되고, 선행 인증을 소비해 사업자 인증 완료 상태로 생성된다.
    // 요청 본문에 사업자번호·대표자명·개업일자 자리가 없다(MyCompanyCreateRequest) — 서버가 인증본에서만 채운다.
    @PostMapping("/api/v1/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IdResponse> createMyCompany(@AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody MyCompanyCreateRequest request) {

        Long id = companyService.createByUser(principal.getUser().getId(), request);
        return ApiResponse.success("회사 등록이 완료되었습니다.", new IdResponse(id));
    }

    // ===== 임시저장(초안) — 발행(POST /companies) 전, 작성 중인 폼 상태를 인증 1건당 1개 보관한다. =====
    // 경로 키는 선행 인증 식별자(verificationId). 'drafts' 는 두 세그먼트라 공개 상세(/{id}, 단일 세그먼트)와 겹치지
    // 않으며, SecurityConfig 의 기본 규칙(anyRequest().authenticated())으로 보호된다.

    // 자동저장(upsert). @Valid 를 붙이지 않아 부분 입력을 그대로 저장한다 — 필수값·마스터 검증은 발행 시점에만 한다.
    // 컬렉션도 요청 본문에 함께 실려 초안 payload 안에 보관되므로, 회사 생성 전까지 한 문서로 관리된다.
    @PutMapping("/api/v1/companies/drafts/{verificationId}")
    public ApiResponse<Void> saveDraft(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable Long verificationId,
                                       @RequestBody MyCompanyCreateRequest request) {
        companyDraftService.save(principal.getUser().getId(), verificationId, request);
        return ApiResponse.ok();
    }

    // 재진입 시 폼 복원 — 저장했던 폼 상태(기본 필드 + 컬렉션)와 마지막 저장 시각을 반환한다. 초안이 없으면 404.
    @GetMapping("/api/v1/companies/drafts/{verificationId}")
    public ApiResponse<CompanyDraftResponse> getDraft(@AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable Long verificationId) {
        return ApiResponse.success(companyDraftService.get(principal.getUser().getId(), verificationId));
    }

    // 임시저장 수동 폐기. 발행이 성공하면 초안은 서버가 자동 삭제하므로(CompanyService.createByUser), 이 경로는
    // 사용자가 작성을 포기할 때만 쓴다. 초안이 없어도 멱등하게 성공한다.
    @DeleteMapping("/api/v1/companies/drafts/{verificationId}")
    public ApiResponse<Void> deleteDraft(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long verificationId) {
        companyDraftService.delete(principal.getUser().getId(), verificationId);
        return ApiResponse.ok();
    }

    // 공개 회사 목록/검색 — 인증 없이 누구나. 통합 키워드 + 고급검색 + 패싯(인증/산업군/카테고리 서브트리).
    // 기본 정렬(spotlight→featured→최신)으로 페이징된 요약 카드를 반환한다.
    @GetMapping("/api/v1/companies")
    public ApiResponse<PageResponse<CompanySummary>> searchCompanies(@AuthenticationPrincipal UserPrincipal principal,
                                                                     @ModelAttribute CompanySearchRequest request,
                                                                     @PageableDefault(size = 20) Pageable pageable) {
        // 인증은 선택: 로그인 상태면 카드마다 좋아요/즐겨찾기 여부를 채우고, 익명이면 principal=null → 전부 false.
        Long viewerId = (principal == null) ? null : principal.getUser().getId();
        return ApiResponse.success(companyQueryService.search(request.toCriteria(), pageable, viewerId));
    }

    // 메인 화면 노출 영역 — 인증 없이 누구나. 스팟라이트·추천 레일을 자리 수만큼 잘라 함께 반환한다.
    // 목록(GET /companies)을 받아 프론트가 플래그로 걸러내는 방식과 달리, 어느 회사가 자리를 차지하는지는
    // 서버가 정한다 — 후보가 자리보다 많아져도 화면이 조용히 비지 않는다.
    // 'my'/'favorites' 와 같은 단일 세그먼트라 공개 상세(/{id})보다 먼저 매칭된다.
    @GetMapping("/api/v1/companies/showcase")
    public ApiResponse<CompanyShowcaseResponse> getShowcase(@AuthenticationPrincipal UserPrincipal principal) {
        // 인증은 선택: 로그인 상태면 카드마다 좋아요/즐겨찾기 여부를 채우고, 익명이면 principal=null → 전부 false.
        Long viewerId = (principal == null) ? null : principal.getUser().getId();
        return ApiResponse.success(companyQueryService.getShowcase(viewerId));
    }

    // 내가 등록한 회사 목록 — 인증된 본인 소유(userId=본인) 미삭제 회사를 요약 카드로, 최신순 페이징(검색 없음).
    // 단일 세그먼트 'my' 라 공개 상세(/{id})보다 먼저 매칭된다(SecurityConfig 에서도 /{id} permitAll 앞에 인증 규칙을 둔다).
    @GetMapping("/api/v1/companies/my")
    public ApiResponse<PageResponse<CompanySummary>> getMyCompanies(@AuthenticationPrincipal UserPrincipal principal,
                                                                    @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(companyQueryService.listMyCompanies(principal.getUser().getId(), pageable));
    }

    // 내 즐겨찾기 회사 목록 — 인증된 본인이 즐겨찾기한 미삭제 회사를 요약 카드로, 즐겨찾기순(담은 최신순) 페이징.
    // 검색/패싯 없음: 즐겨찾기는 회사 속성이 아니라 뷰어↔회사 관계라 공개 검색(GET /companies)의 조건으로 넣지 않고
    // 별도 경로로 둔다(정렬 키 f.created_at 도 검색 쿼리엔 없다). 'my' 와 같은 단일 세그먼트라 /{id} 보다 먼저 매칭된다.
    @GetMapping("/api/v1/companies/favorites")
    public ApiResponse<PageResponse<CompanySummary>> getMyFavorites(@AuthenticationPrincipal UserPrincipal principal,
                                                                    @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(companyQueryService.listMyFavorites(principal.getUser().getId(), pageable));
    }

    // 일반(공개) 상세 조회 — 누구에게나 안전한 공개 필드만 반환한다. 소프트 삭제된 회사는 404.
    // 인증은 선택: 로그인 상태면 principal 로 좋아요/즐겨찾기 여부(likedByMe/favoritedByMe)를 채우고, 익명이면 principal=null → false.
    @GetMapping("/api/v1/companies/{id}")
    public ApiResponse<CompanyPublicResponse> getCompany(@AuthenticationPrincipal UserPrincipal principal,
                                                         @PathVariable Long id) {
        Long viewerId = (principal == null) ? null : principal.getUser().getId();
        return ApiResponse.success(companyQueryService.getPublic(id, viewerId));
    }

    // 소유자 전용 상세 조회 — 요청자가 해당 회사의 멤버여야 하며, 내부·운영 메타(meta)까지 포함한다.
    @GetMapping("/api/v1/companies/{id}/private")
    public ApiResponse<CompanyDetailResponse> getMyCompany(@AuthenticationPrincipal UserPrincipal principal,
                                                           @PathVariable Long id) {
        return ApiResponse.success(companyQueryService.getOwnerView(id, principal.getUser().getId()));
    }

    // 소유자 전용 구독 조회 — 요청자가 해당 회사의 멤버여야 한다. 회사 데이터와 섞지 않고 구독 정보만 단독으로 반환한다.
    @GetMapping("/api/v1/companies/{id}/subscription")
    public ApiResponse<CompanySubscriptionResponse> getMySubscription(@AuthenticationPrincipal UserPrincipal principal,
                                                                      @PathVariable Long id) {
        return ApiResponse.success(companyQueryService.getSubscriptionForOwner(id, principal.getUser().getId()));
    }

    // 기본 정보 부분 수정 — 소유자만. null=미변경(sparse). 수정 값은 화면에 이미 반영되므로 본문 없이 성공만 반환한다.
    @PatchMapping("/api/v1/companies/{id}")
    public ApiResponse<Void> updateMyCompany(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long id,
                                             @Valid @RequestBody CompanyUpdateRequest request) {
        companyUpdateService.updateBasicInfoByUser(id, principal.getUser().getId(), request);
        return ApiResponse.ok();
    }

    // 공개/비공개 전환 — 소유자만. 목표 상태(PUBLIC/PRIVATE)를 지정한다(멱등). 응답은 본문 없이 성공만.
    @PatchMapping("/api/v1/companies/{id}/visibility")
    public ApiResponse<Void> changeVisibility(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long id,
                                              @Valid @RequestBody CompanyVisibilityUpdateRequest request) {
        companyUpdateService.changeVisibilityByUser(id, principal.getUser().getId(), request.visibility());
        return ApiResponse.ok();
    }

    // 소유자 자가 삭제(소프트) — 본인 소유 회사를 숨김 처리한다. 멱등(이미 삭제여도 성공). 삭제 후에는 소유자 목록·공개
    // 경로에서 사라지고 관리자에게만 보이므로, 되살리기는 관리자 복구 경로에서만 한다(<<admin-company-restore>>).
    @DeleteMapping("/api/v1/companies/{id}")
    public ApiResponse<Void> deleteMyCompany(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long id) {
        companyUpdateService.deleteByUser(id, principal.getUser().getId());
        return ApiResponse.ok();
    }

    // ===== 컬렉션 전체 교체(PUT) — 소유자만. 각 컬렉션을 통째로 새 리스트로 교체한다(빈 리스트 = 전부 비우기). =====
    // 표시 순서(displayOrder)는 클라이언트가 보내지 않고 서버가 리스트 인덱스로 재부여한다. 응답은 본문 없이 성공만.

    @PutMapping("/api/v1/companies/{id}/tags")
    public ApiResponse<Void> replaceTags(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long id, @RequestBody List<String> tagNames) {
        companyUpdateService.replaceTagsByUser(id, principal.getUser().getId(), tagNames);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/materials")
    public ApiResponse<Void> replaceMaterials(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long id, @RequestBody List<String> materialNames) {
        companyUpdateService.replaceMaterialsByUser(id, principal.getUser().getId(), materialNames);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/equipment")
    public ApiResponse<Void> replaceEquipment(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long id, @RequestBody List<String> equipmentNames) {
        companyUpdateService.replaceEquipmentByUser(id, principal.getUser().getId(), equipmentNames);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/images")
    public ApiResponse<Void> replaceGalleryImages(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable Long id, @Valid @RequestBody List<ImageRequest> images) {
        companyUpdateService.replaceGalleryImagesByUser(id, principal.getUser().getId(), images);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/contacts")
    public ApiResponse<Void> replaceContacts(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long id, @Valid @RequestBody List<ContactRequest> contacts) {
        companyUpdateService.replaceContactsByUser(id, principal.getUser().getId(), contacts);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/references")
    public ApiResponse<Void> replaceReferences(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable Long id, @Valid @RequestBody List<ReferenceRequest> references) {
        companyUpdateService.replaceReferencesByUser(id, principal.getUser().getId(), references);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/categories")
    public ApiResponse<Void> replaceCategories(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable Long id, @RequestBody List<Long> categoryIds) {
        companyUpdateService.replaceCategoriesByUser(id, principal.getUser().getId(), categoryIds);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/industries")
    public ApiResponse<Void> replaceIndustries(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable Long id, @RequestBody List<Long> industryIds) {
        companyUpdateService.replaceIndustriesByUser(id, principal.getUser().getId(), industryIds);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/certifications")
    public ApiResponse<Void> replaceCertifications(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long id, @RequestBody List<Long> certificationIds) {
        companyUpdateService.replaceCertificationsByUser(id, principal.getUser().getId(), certificationIds);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/countries")
    public ApiResponse<Void> replaceCountries(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long id, @RequestBody List<Long> countryIds) {
        companyUpdateService.replaceCountriesByUser(id, principal.getUser().getId(), countryIds);
        return ApiResponse.ok();
    }

    @PutMapping("/api/v1/companies/{id}/regions")
    public ApiResponse<Void> replaceRegions(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable Long id, @RequestBody List<Long> domesticRegionIds) {
        companyUpdateService.replaceRegionsByUser(id, principal.getUser().getId(), domesticRegionIds);
        return ApiResponse.ok();
    }
}
