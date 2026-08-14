package project.plantly.global.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.enums.SubscriptionStatus;
import project.plantly.domain.company.policy.GradePolicyRegistry;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 정족수 패딩.
 *
 * <p>페이징 엔드포인트 4개가 전부 {@code @PageableDefault(size = 20)} 이다. 그런데 페이지네이션은
 * <b>테이블이 아니라 뷰마다</b> 검증해야 한다 — 회사를 아무리 많이 넣어도 그게 전부 관리자 등록이면
 * {@code /companies/my} 는 여전히 한 페이지다. 그래서 소유자별 정족수를 따로 만든다.
 *
 * <ul>
 *   <li>P01~P12 — U1 자가등록. 계약 케이스의 U1 소유분과 합쳐 {@code /companies/my} 가
 *       {@code ?size=5} 에서 3페이지 이상 나온다.</li>
 *   <li>P13~P24 — 관리자 등록. 공개 목록을 기본 size=20 에서 3페이지까지 밀어 올린다.</li>
 * </ul>
 *
 * <p>케이스 회사와 달리 이름에 코드를 붙이지 않는다 — 목록 화면이 실제 서비스처럼 보여야 훑어보며
 * 어색한 지점을 발견할 수 있고, 코드가 붙은 행은 그 자체로 눈에 띄어 케이스 표본과 구분된다.
 */
@Slf4j
@Component
@Profile(SeedConfig.PROFILE)
@RequiredArgsConstructor
public class SeedPadding {

    /** U1 이 자가등록으로 소유하는 패딩 수. 케이스 소유분(C01/C02/D06)과 합쳐 /companies/my 를 채운다. */
    private static final int OWNED_COUNT = 12;

    /** 관리자 등록 패딩 수. 공개 목록 정족수(케이스 공개분 + 이 수 >= 41)를 맞춘다. */
    private static final int ADMIN_COUNT = 12;

    // 패딩에도 등급을 섞어야 목록 카드가 한 가지 모양으로만 보이지 않는다.
    // 부수 효과: PREMIUM/ENTERPRISE 로 배정되는 절반(6건)은 유료 활성 구독이라 메인 스팟라이트 후보가 된다.
    // 자리(5칸)를 훨씬 넘기므로 초과 상황이 시드만 올려도 재현된다(SeedCompanyCases 의 C18/C19 근처 주석 참고).
    private static final List<CompanyGrade> ADMIN_GRADES = List.of(
            CompanyGrade.BASIC, CompanyGrade.STANDARD, CompanyGrade.PREMIUM, CompanyGrade.ENTERPRISE);

    private final SeedCompanyFactory companies;
    private final SeedVerificationFactory verifications;
    private final SeedMasterCatalog masters;
    private final GradePolicyRegistry gradePolicyRegistry;

    public Result create(SeedAccounts accounts) {
        List<SeedCompanyRef> owned = new ArrayList<>();
        List<SeedCompanyRef> adminRegistered = new ArrayList<>();
        SeedAccount owner = accounts.owner1();
        Long adminId = accounts.admin().userId();
        LocalDate today = LocalDate.now();

        for (int n = 1; n <= OWNED_COUNT; n++) {
            int index = SeedIndexes.forPadding(n);
            Long verificationId = verifications.issue(
                    owner.userId(),
                    SeedBusinessNumbers.paddingNumber(n),
                    SeedVocabulary.ceoName(index),
                    SeedVocabulary.establishmentDate(index));

            Long companyId = companies.createByUser(owner.userId(),
                    SeedCompanyRequestBuilder.of(index, masters)
                            .limitTo(gradePolicyRegistry.of(CompanyGrade.FREE), masters)
                            .buildMy(verificationId));

            owned.add(new SeedCompanyRef(code(n), companyId, owner.code(),
                    "정족수 패딩(U1 자가등록). /companies/my 페이지네이션과 공개 목록을 함께 채운다"));
        }

        for (int n = 1; n <= ADMIN_COUNT; n++) {
            int sequence = OWNED_COUNT + n;
            int index = SeedIndexes.forPadding(sequence);
            CompanyGrade grade = ADMIN_GRADES.get(Math.floorMod(n, ADMIN_GRADES.size()));

            Long companyId = companies.createByAdmin(adminId,
                    SeedCompanyRequestBuilder.of(index, masters)
                            .businessNumber(SeedBusinessNumbers.paddingNumber(sequence))
                            .limitTo(gradePolicyRegistry.of(grade), masters)
                            .build());
            companies.changeSubscription(companyId, grade, SubscriptionStatus.ACTIVE, today.plusYears(1));

            adminRegistered.add(new SeedCompanyRef(code(sequence), companyId, null,
                    "정족수 패딩(관리자 등록, " + grade + "). 공개 목록을 기본 size=20 기준 3페이지까지 채운다"));
        }

        log.info("[seed] 패딩 회사 {}건 생성 (U1 소유 {}, 관리자 등록 {})",
                owned.size() + adminRegistered.size(), owned.size(), adminRegistered.size());
        return new Result(owned, adminRegistered);
    }

    /** 즐겨찾기 대상으로는 관리자 등록분만 쓴다 — 남의 회사를 찜하는 것이 실제 사용 흐름이다. */
    public record Result(List<SeedCompanyRef> owned, List<SeedCompanyRef> adminRegistered) {

        public List<SeedCompanyRef> all() {
            List<SeedCompanyRef> merged = new ArrayList<>(owned);
            merged.addAll(adminRegistered);
            return merged;
        }
    }

    private String code(int sequence) {
        return "P" + String.format("%02d", sequence);
    }
}
