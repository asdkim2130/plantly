package project.plantly.global.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.category.Category;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.enums.CompanyVisibility;
import project.plantly.domain.company.enums.SubscriptionStatus;
import project.plantly.domain.company.policy.GradePolicyRegistry;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * 계약 케이스 회사 C01~C25.
 *
 * <p>이 목록이 시드의 본체다. 각 행은 채우기 위한 데이터가 아니라 <b>프론트-백엔드 계약의 한 갈래를
 * 증명하기 위한 표본</b>이고, 회사명 앞의 케이스 코드가 그 표본의 안정 키다(id 는 시드할 때마다 바뀐다).
 *
 * <p>축은 넷이다 — 공개/비공개, 관리자등록/자가등록, 구독 등급, 그리고 삭제·인증·큐레이션 같은 상태 플래그.
 * 등록 시점에 지정할 수 없는 값(만료된 구독, 소프트 삭제, 운영 플래그)은 등록 후 도메인 메서드로 바꾼다.
 */
@Slf4j
@Component
@Profile(SeedConfig.PROFILE)
@RequiredArgsConstructor
public class SeedCompanyCases {

    private final SeedCompanyFactory companies;
    private final SeedVerificationFactory verifications;
    private final SeedMasterCatalog masters;
    private final GradePolicyRegistry gradePolicyRegistry;

    // 자가등록이 실제로 받는 등급. InitialSubscriptionPolicy 와 같은 값을 시드가 알아야 요청을 그 한도로 채울 수 있다.
    // (정책을 주입해 물어보지 않는 이유: 정책은 CompanyVerification 을 받는데, 시드는 요청을 만드는 시점에 아직 없다)
    private static final CompanyGrade SELF_REGISTRATION_GRADE = CompanyGrade.ENTERPRISE;

    public List<SeedCompanyRef> create(SeedAccounts accounts) {
        List<SeedCompanyRef> refs = new ArrayList<>();
        Long adminId = accounts.admin().userId();
        LocalDate today = LocalDate.now();

        // ===== 자가등록(USER). 소유자 멤버십 + businessVerified 가 실제 경로로 채워진다.
        //       국세청 인증을 직접 통과한 등록이라 구독은 체험 ENTERPRISE 로 시작한다. =====

        refs.add(userCase(accounts.owner1(), 1, "기본공개",
                "기준선. 공개 + 자가등록 + 사업자 인증 완료 상태의 카드·상세·소유자 뷰", b -> b));

        Long privateUserCompany = userCompany(accounts.owner1(), 2, "비공개",
                b -> b.visibility(CompanyVisibility.PRIVATE));
        // 자가등록은 체험 ENTERPRISE 로만 시작한다. 다른 등급의 유저 회사를 보려면 등록 후 구독을 바꿔야 한다.
        companies.changeSubscription(privateUserCompany, CompanyGrade.STANDARD, SubscriptionStatus.ACTIVE, today.plusYears(1));
        refs.add(new SeedCompanyRef("C02", privateUserCompany, accounts.owner1().code(),
                "비공개 회사. 익명·타인에게는 목록/상세에서 빠지고 소유자와 관리자에게만 보여야 한다 (구독 STANDARD)"));

        Long deletedUserCompany = userCompany(accounts.owner1(), 3, "삭제됨", b -> b);
        companies.softDelete(deletedUserCompany);
        refs.add(new SeedCompanyRef("C03", deletedUserCompany, accounts.owner1().code(),
                "소프트 삭제. 공개 목록·상세에서 제외되고 관리자 목록에만 남아 복구 대상이 된다"));

        Long deletedPrivateCompany = userCompany(accounts.owner1(), 4, "비공개삭제",
                b -> b.visibility(CompanyVisibility.PRIVATE));
        companies.softDelete(deletedPrivateCompany);
        refs.add(new SeedCompanyRef("C04", deletedPrivateCompany, accounts.owner1().code(),
                "비공개 + 삭제. 두 필터가 동시에 걸렸을 때도 관리자 목록에서는 보여야 한다"));

        // ===== 관리자등록(ADMIN). 소유자 미연동(CompanyMember 0건) + ADMIN_EXEMPT 구독. =====

        refs.add(adminCase(adminId, 5, "관리자등록", CompanyGrade.ENTERPRISE,
                "관리자 대신등록(unclaimed). 공개 목록엔 나오지만 어느 계정의 '내 회사'에도 없고 소유자 뷰가 없다",
                b -> b));

        refs.add(adminCase(adminId, 6, "관리자등록비공개", CompanyGrade.ENTERPRISE,
                "관리자등록 + 비공개. 소유자가 없으므로 관리자만 볼 수 있다",
                b -> b.visibility(CompanyVisibility.PRIVATE)));

        refs.add(userCase(accounts.owner2(), 7, "타인소유",
                "U2 소유. U1 로 로그인해 이 회사를 수정·삭제하려 하면 차단되어야 한다", b -> b));

        // ===== 등급 5종. 관리자등록으로 만든 뒤 구독을 교체한다(등록 시점엔 등급을 고를 수 없다). =====

        refs.add(gradeCase(adminId, 8, CompanyGrade.FREE, today,
                "FREE 등급 표시. 카테고리 1개, 상세이미지 3장, 동영상 없음"));
        refs.add(gradeCase(adminId, 9, CompanyGrade.BASIC, today,
                "BASIC 등급 표시. 카테고리 2개, 동영상 없음"));
        refs.add(gradeCase(adminId, 10, CompanyGrade.STANDARD, today,
                "STANDARD 등급 표시. 카테고리 5개, 동영상 허용"));
        refs.add(gradeCase(adminId, 11, CompanyGrade.PREMIUM, today,
                "PREMIUM 등급 표시. 카테고리 10개, 스팟라이트 자격"));
        refs.add(gradeCase(adminId, 12, CompanyGrade.ENTERPRISE, today,
                "ENTERPRISE 등급 표시. 레퍼런스 이미지까지 허용되는 최상위"));

        // ===== 구독 상태 파생. effectiveGrade 는 저장값이 아니라 파생값이라 프론트가 틀리기 쉬운 지점이다. =====

        Long expired = adminCompany(adminId, 13, "만료구독", CompanyGrade.PREMIUM, b -> b);
        companies.changeSubscription(expired, CompanyGrade.PREMIUM, SubscriptionStatus.ACTIVE, today.minusDays(10));
        refs.add(new SeedCompanyRef("C13", expired, null,
                "만료된 PREMIUM 구독. 저장 등급은 PREMIUM 이지만 effectiveGrade 는 FREE 로 강등되어야 한다"
                        + " (컬렉션은 PREMIUM 시절 그대로라 한도 초과 상태로 남는다)"));

        Long expiringSoon = adminCompany(adminId, 14, "만료임박", CompanyGrade.PREMIUM, b -> b);
        companies.changeSubscription(expiringSoon, CompanyGrade.PREMIUM, SubscriptionStatus.ACTIVE, today.plusDays(3));
        refs.add(new SeedCompanyRef("C14", expiringSoon, null,
                "3일 뒤 만료. 아직 PREMIUM 이지만 만료 임박 안내를 띄울 수 있어야 한다"));

        Long trial = adminCompany(adminId, 15, "체험판", CompanyGrade.STANDARD, b -> b);
        companies.changeSubscription(trial, CompanyGrade.STANDARD, SubscriptionStatus.TRIAL, today.plusDays(14));
        refs.add(new SeedCompanyRef("C15", trial, null,
                "TRIAL 상태. 등급 혜택은 STANDARD 와 같지만 상태 배지가 달라야 한다"));

        // ===== 인증·큐레이션 플래그 =====

        Long unverified = adminCompany(adminId, 16, "미인증", CompanyGrade.BASIC, b -> b);
        companies.changeSubscription(unverified, CompanyGrade.BASIC, SubscriptionStatus.ACTIVE, today.plusYears(1));
        refs.add(new SeedCompanyRef("C16", unverified, null,
                "관리자 검수(verified)·사업자 인증(businessVerified) 모두 없음. 배지가 하나도 붙지 않아야 한다"));

        Long featured = adminCompany(adminId, 17, "추천", CompanyGrade.PREMIUM, b -> b);
        companies.changeSubscription(featured, CompanyGrade.PREMIUM, SubscriptionStatus.ACTIVE, today.plusYears(1));
        companies.applyFlags(featured, true, true, false, 0);
        refs.add(new SeedCompanyRef("C17", featured, null,
                "featured=true + verified=true. 추천 섹션과 에디터 선정 배지"));

        Long spotlight1 = adminCompany(adminId, 18, "스팟라이트1", CompanyGrade.PREMIUM, b -> b);
        companies.changeSubscription(spotlight1, CompanyGrade.PREMIUM, SubscriptionStatus.ACTIVE, today.plusYears(1));
        companies.applyFlags(spotlight1, true, false, true, 1);
        refs.add(new SeedCompanyRef("C18", spotlight1, null,
                "관리자 수동 고정(pin), 순서 1. 메인 스팟라이트에서 C19 보다 앞, 요금제 자격분(C11/C12 등)보다 앞에 와야 한다"));

        // 커버·브랜드 컬러를 일부러 비운다. 스팟라이트 카드는 커버를 배경으로, 브랜드 컬러를 배너 색으로 쓰는데
        // 둘 다 선택 필드라 없는 회사가 실제로 레일에 오른다 — 폴백(자리표시자 + 기본 배너 색)이 그려지는지
        // 개발 단계에서 눈으로 확인할 자리가 필요하다. C18 이 둘 다 갖춘 케이스라 나란히 비교된다.
        Long spotlight2 = adminCompany(adminId, 19, "스팟라이트2", CompanyGrade.ENTERPRISE,
                SeedCompanyRequestBuilder::withoutCoverStyling);
        companies.changeSubscription(spotlight2, CompanyGrade.ENTERPRISE, SubscriptionStatus.ACTIVE, today.plusYears(1));
        companies.applyFlags(spotlight2, true, false, true, 2);
        refs.add(new SeedCompanyRef("C19", spotlight2, null,
                "관리자 수동 고정(pin), 순서 2. C18 다음에 와야 한다."
                        + " 커버 이미지·브랜드 컬러가 없어 스팟라이트 카드가 폴백으로 그려져야 한다"));

        // 메인 스팟라이트 자리 초과(app.showcase.spotlight-slots=5): 후보가 12건이라 7건이 잘린다.
        //  - pin 2건: C18·C19
        //  - 요금제 자격(유료 ACTIVE PREMIUM/ENTERPRISE 미만료) 4건: C11·C12·C14·C17
        //  - 정족수 패딩 6건: SeedPadding 의 관리자 등록 12건 중 PREMIUM/ENTERPRISE 로 배정된 절반
        // 만료 구독(C13)·체험(C15)·ADMIN_EXEMPT(C05/C06/C21)는 자격이 없어 후보에서 빠진다.
        // 시드를 올리면 CompanyQueryService 의 초과 경고 로그가 실제로 뜬다 — 로테이션 없이 초과 상황의
        // 동작(잘림 순서)과 감지 장치가 함께 작동하는지 개발 단계에서 눈으로 확인하기 위한 구성이다.

        // ===== 렌더링 경계 =====

        Long empty = adminCompany(adminId, 20, "빈컬렉션", CompanyGrade.FREE,
                b -> b.withoutCollections().withoutOptionalFields());
        companies.changeSubscription(empty, CompanyGrade.FREE, SubscriptionStatus.ACTIVE, null);
        refs.add(new SeedCompanyRef("C20", empty, null,
                "연락처·레퍼런스·이미지·소재·장비·태그 전부 0건 + 선택 필드 전부 null."
                        + " 로고와 필수값만 있는 최소 회사 (빈 섹션 렌더링)"));

        Long maxed = adminCompany(adminId, 21, "최대치", CompanyGrade.ENTERPRISE,
                b -> b.categoryIds(masters.categoryIds(SeedIndexes.forCase(21), 10))
                        .certificationIds(masters.certificationIds(SeedIndexes.forCase(21), 6))
                        // 마스터 목록에 없는 인증을 직접 적어 넣은 링크. 같은 '기타' 마스터에 이름만 달리해 2건이 붙는다
                        // — 배지가 마스터 이름("기타")이 아니라 입력한 이름으로 뜨는지 확인하는 자리다.
                        .customCertificationNames(masters.etcCertificationId(),
                                List.of("사내 표준 품질인증 QM-2024", "○○산업협회 우수기업 인증"))
                        .countryIds(masters.countryIds(SeedIndexes.forCase(21), 8))
                        .industryIds(masters.industryIds(SeedIndexes.forCase(21), 4))
                        .domesticRegionIds(masters.regionIds(SeedIndexes.forCase(21), 5))
                        .detailImageCount(30)
                        .referenceImageCount(10));
        refs.add(new SeedCompanyRef("C21", maxed, null,
                "ENTERPRISE 한도를 꽉 채운 회사(카테고리 10, 인증 6 + 직접입력 2, 상세이미지 30, 레퍼런스 이미지 10, 지역 5, 국가 8)."
                        + " 배지·갤러리 오버플로 처리 확인"));

        Long longText = adminCompany(adminId, 22, "긴이름", CompanyGrade.STANDARD,
                b -> b.companyName("[C22] " + "가나다라마바사아자차카타파하".repeat(3) + "정밀기계공업주식회사")
                        .introTitle("한 줄 요약이 한 줄에 담기지 않을 만큼 길어졌을 때 카드와 상세에서 어떻게 잘리는지 확인하기 위한 문장입니다")
                        .content(longContent()));
        companies.changeSubscription(longText, CompanyGrade.STANDARD, SubscriptionStatus.ACTIVE, today.plusYears(1));
        refs.add(new SeedCompanyRef("C22", longText, null,
                "회사명·한줄요약·소개글이 모두 과도하게 긴 회사. 말줄임·줄바꿈·레이아웃 붕괴 확인"));

        // ===== 검색·패싯 =====

        Category leaf = masters.leafCategory(SeedIndexes.forCase(23));
        Category root = masters.rootAncestorOf(leaf);
        Long leafOnly = adminCompany(adminId, 23, "소분류전용", CompanyGrade.BASIC,
                b -> b.categoryIds(List.of(leaf.getId())));
        companies.changeSubscription(leafOnly, CompanyGrade.BASIC, SubscriptionStatus.ACTIVE, today.plusYears(1));
        refs.add(new SeedCompanyRef("C23", leafOnly, null,
                "소분류 '" + leaf.getCategoryName() + "' 하나에만 연결. 대분류 '" + root.getCategoryName()
                        + "'(id=" + root.getId() + ")로 필터해도 closure 를 타고 잡혀야 한다"));

        Long equipmentOnly = adminCompany(adminId, 24, "장비명검색", CompanyGrade.BASIC,
                b -> b.equipmentNames(List.of(EQUIPMENT_ONLY_TOKEN, "CNC 선반")));
        companies.changeSubscription(equipmentOnly, CompanyGrade.BASIC, SubscriptionStatus.ACTIVE, today.plusYears(1));
        refs.add(new SeedCompanyRef("C24", equipmentOnly, null,
                "'" + EQUIPMENT_ONLY_TOKEN + "' 토큰이 장비명에만 있고 회사명·소개글에는 없다."
                        + " 이 키워드로 검색해 잡히면 자식 텍스트(equipment_text)까지 색인된 것이다"));

        // ===== 등급 재조정(비활성 항목) =====
        // 저장은 살아 있지만 공개에서 빠진 항목이 있는 회사. 다운그레이드·체험 만료 후의 상태를 미리 만들어 둔 것이다.
        // 프로덕션 경로로는 아직 이 상태가 만들어지지 않는다(자가등록은 최상위 등급이고 재조정 배치가 없다) —
        // 이 케이스가 없으면 조회·카드·색인에 넣은 active 필터가 한 번도 발동하지 않아 동작을 확인할 수 없다.
        Long downgraded = adminCompany(adminId, 25, "강등후초과", CompanyGrade.FREE,
                b -> b.categoryIds(masters.categoryIds(SeedIndexes.forCase(25), 5))
                        .detailImageCount(6));
        companies.changeSubscription(downgraded, CompanyGrade.FREE, SubscriptionStatus.ACTIVE, null);
        // FREE 한도(카테고리 1, 상세이미지 3)만 남기고 나머지는 끈다.
        companies.deactivateOverflow(downgraded, 1, 3);
        refs.add(new SeedCompanyRef("C25", downgraded, null,
                "카테고리 5건 중 1건, 상세이미지 6장 중 3장만 활성. 공개 상세·카드·패싯 검색에서는 꺼진 항목이"
                        + " 아예 빠지고, 관리자/소유자 상세에서는 전부 내려오되 active=false 로 구분돼야 한다"));

        log.info("[seed] 계약 케이스 회사 {}건 생성", refs.size());
        return refs;
    }

    /** 통합검색이 자식 테이블까지 훑는지 확인하는 고유 토큰. 다른 어떤 필드에도 등장하지 않아야 한다. */
    static final String EQUIPMENT_ONLY_TOKEN = "젠틀리테스트가공기";

    // ===== 생성 헬퍼 =====

    private SeedCompanyRef userCase(SeedAccount owner, int caseNo, String label, String proves,
                                    UnaryOperator<SeedCompanyRequestBuilder> customize) {
        Long companyId = userCompany(owner, caseNo, label, customize);
        return new SeedCompanyRef(code("C", caseNo), companyId, owner.code(), proves);
    }

    /**
     * 자가등록. 선행 인증을 먼저 발급하고 그것을 소비해 만든다 — 인증 소비, 초안 삭제, OWNER 멤버십,
     * businessVerified 가 전부 실제 등록 트랜잭션 안에서 일어난다.
     *
     * <p>국세청 인증을 직접 통과한 등록이라 구독은 체험 등급(ENTERPRISE)으로 시작한다
     * ({@code InitialSubscriptionPolicy}). {@code limitTo} 로 그 한도에 맞추는 것은 여전히 필요하다 —
     * 최상위 등급이라 실제로 걸릴 일은 없지만, 시드가 등급 표를 읽어 채우면 표를 조정했을 때 시드도 따라온다.
     */
    private Long userCompany(SeedAccount owner, int caseNo, String label,
                             UnaryOperator<SeedCompanyRequestBuilder> customize) {
        int index = SeedIndexes.forCase(caseNo);
        Long verificationId = verifications.issue(
                owner.userId(),
                SeedBusinessNumbers.caseNumber(caseNo),
                SeedVocabulary.ceoName(index),
                SeedVocabulary.establishmentDate(index));

        SeedCompanyRequestBuilder builder = SeedCompanyRequestBuilder.of(index, masters)
                .companyName(named(caseNo, label, index))
                .limitTo(gradePolicyRegistry.of(SELF_REGISTRATION_GRADE), masters);

        return companies.createByUser(owner.userId(), customize.apply(builder).buildMy(verificationId));
    }

    /** 등급 케이스: 관리자등록으로 만들고 구독만 목표 등급으로 교체한다. */
    private SeedCompanyRef gradeCase(Long adminId, int caseNo, CompanyGrade grade, LocalDate today, String proves) {
        Long companyId = adminCompany(adminId, caseNo, grade.name(), grade, b -> b);
        // FREE 는 무기한(구독 없음과 동치)이라 만료일을 두지 않는다.
        LocalDate expiresAt = grade == CompanyGrade.FREE ? null : today.plusYears(1);
        companies.changeSubscription(companyId, grade, SubscriptionStatus.ACTIVE, expiresAt);
        return new SeedCompanyRef(code("C", caseNo), companyId, null, proves);
    }

    private SeedCompanyRef adminCase(Long adminId, int caseNo, String label, CompanyGrade lookLike, String proves,
                                     UnaryOperator<SeedCompanyRequestBuilder> customize) {
        Long companyId = adminCompany(adminId, caseNo, label, lookLike, customize);
        return new SeedCompanyRef(code("C", caseNo), companyId, null, proves);
    }

    /**
     * 관리자등록. 구독이 ADMIN_EXEMPT 라 등급 정책이 전부 스킵된다(요청 DTO 의 절대 천장은 여전히 적용된다).
     * 그래도 {@code lookLike} 등급 한도에 맞춰 채우는 이유는, 등급별 화면 차이를 보려면 데이터도
     * 그 등급처럼 생겨야 하기 때문이다.
     */
    private Long adminCompany(Long adminId, int caseNo, String label, CompanyGrade lookLike,
                              UnaryOperator<SeedCompanyRequestBuilder> customize) {
        int index = SeedIndexes.forCase(caseNo);
        SeedCompanyRequestBuilder builder = SeedCompanyRequestBuilder.of(index, masters)
                .businessNumber(SeedBusinessNumbers.caseNumber(caseNo))
                .companyName(named(caseNo, label, index))
                .limitTo(gradePolicyRegistry.of(lookLike), masters);

        return companies.createByAdmin(adminId, customize.apply(builder).build());
    }

    /** 케이스 코드를 회사명 앞에 박아 목록에서 바로 식별되게 한다. 패딩 회사에는 붙이지 않는다. */
    private String named(int caseNo, String label, int index) {
        return "[" + code("C", caseNo) + "] " + SeedVocabulary.companyName(index) + label;
    }

    private String code(String prefix, int number) {
        return prefix + String.format("%02d", number);
    }

    private String longContent() {
        return ("정밀 가공 분야에서 축적한 경험을 바탕으로 시제품 제작부터 양산까지 전 공정을 대응합니다. "
                + "도면 검토 단계에서 가공성 개선안을 함께 제안하며, 초도 물량은 전수 검사 후 납품합니다. ")
                .repeat(12);
    }
}
