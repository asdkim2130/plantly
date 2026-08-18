package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.plantly.domain.company.dto.CompanyReverificationRequest;
import project.plantly.domain.company.dto.CompanyReverificationResponse;
import project.plantly.domain.company.dto.CompanyVerificationRequest;
import project.plantly.domain.company.dto.CompanyVerificationResponse;
import project.plantly.domain.company.entity.CompanyVerification;
import project.plantly.domain.company.enums.VerificationOutcome;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.nts.NtsClient;
import project.plantly.domain.company.policy.GradePolicyRegistry;
import project.plantly.domain.company.policy.InitialSubscriptionPolicy;
import project.plantly.domain.company.support.BusinessNumbers;
import project.plantly.global.exception.BusinessException;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 회사 등록 전 사업자 인증(선행 인증).
 *
 * <p>클래스 레벨 @Transactional 이 없는 것은 의도다 — 이 메서드 한가운데에 국세청 HTTP 호출이 있고,
 * 그걸 트랜잭션으로 감싸면 외부 응답을 기다리는 동안 DB 커넥션을 점유한다. DB 작업은 전부
 * {@link CompanyVerificationWriter} 의 짧은 트랜잭션으로 위임한다.
 */
@Service
@RequiredArgsConstructor
public class CompanyVerificationService {

    // 국세청 API 일일 쿼터 보호 + 대표자명을 바꿔가며 찔러보는 시도 차단.
    private static final int DAILY_ATTEMPT_LIMIT = 5;

    // 발급된 인증의 유효기간. 인증만 받아두고 방치한 레코드가 영원히 유효하면, 그 사이 사업자 상태가
    // 바뀌어도(폐업 등) 옛 판정으로 등록할 수 있게 된다.
    private static final Duration VERIFICATION_TTL = Duration.ofMinutes(30);

    private final NtsClient ntsClient;
    private final CompanyVerificationWriter writer;

    // 발급 응답에 실을 초기 등급/한도의 출처. 회사 생성(CompanyService)이 실제 구독을 만들 때 읽는 것과
    // 같은 정책이라, 폼에 안내한 한도와 서버 검증이 구조적으로 어긋날 수 없다.
    private final InitialSubscriptionPolicy initialSubscriptionPolicy;
    private final GradePolicyRegistry gradePolicyRegistry;

    public CompanyVerificationResponse verify(Long userId, CompanyVerificationRequest request) {
        // 국세청에 물어보기 전에 형식으로 거른다 — 오타로 쿼터를 태우지 않는다.
        String businessNumber = BusinessNumbers.normalize(request.businessNumber());
        if (businessNumber == null) {
            throw new BusinessException(CompanyErrorCode.BUSINESS_NUMBER_INVALID_FORMAT);
        }

        LocalDateTime now = LocalDateTime.now();
        long used = writer.countTodayAttempts(userId, now.toLocalDate().atStartOfDay());
        if (used >= DAILY_ATTEMPT_LIMIT) {
            throw new BusinessException(CompanyErrorCode.VERIFICATION_LIMIT_EXCEEDED);
        }

        NtsClient.Result result;
        try {
            result = ntsClient.verify(businessNumber, request.ceoName(), request.businessStartDate());
        } catch (NtsClient.UnavailableException e) {
            // 국세청 쪽 문제다. 감사 로그엔 남기되 재시도 제한은 소모시키지 않고, 사용자에게도
            // 입력 오류가 아니라 시스템 지연임을 알린다.
            writer.recordAttempt(userId, businessNumber, request.ceoName(), request.businessStartDate(),
                    VerificationOutcome.UNAVAILABLE, null);
            throw new BusinessException(CompanyErrorCode.VERIFICATION_UNAVAILABLE);
        }

        if (!result.isValid()) {
            writer.recordAttempt(userId, businessNumber, request.ceoName(), request.businessStartDate(),
                    result.outcome(), result.rawResponse());
            throw new BusinessException(toErrorCode(result.outcome()));
        }

        CompanyVerification verification = writer.issue(userId, businessNumber, request.ceoName(),
                request.businessStartDate(), result.rawResponse(), now, VERIFICATION_TTL);

        // 이 인증으로 만들 회사가 받을 등급을 지금 확정해 폼에 함께 내린다. 회사가 생기기 전이라
        // '등급을 알려면 회사가 있어야 한다'는 순서 문제가 여기서 풀린다(등급 = 인증의 결과).
        CompanyGrade initialGrade = initialSubscriptionPolicy.initialGrade(verification);
        return CompanyVerificationResponse.from(verification, initialGrade, gradePolicyRegistry.of(initialGrade));
    }

    /**
     * 사업자 재인증. 이미 등록·인증된 회사의 대표자명·개업일자를 국세청에 다시 확인해 갱신한다.
     *
     * <p>선행 인증(verify)과 결정적으로 다른 점은 <b>사업자번호를 요청으로 받지 않는다는 것</b>이다. 번호는
     * DB 저장값(최초 인증본에서 온 불변값)을 그대로 국세청에 질의한다 — 재인증 때 번호를 새로 입력받으면
     * 타사 번호로 배지를 갈아끼울 수 있기 때문이다. 국세청 정보가 바뀌었을 수 있으므로 이전 값과 비교하지
     * 않고, 새 대표자명·개업일자가 통과하면 그대로 DB 를 덮어쓰고 인증 시각을 갱신한다.
     *
     * <p>트랜잭션 경계는 verify 와 동일하다 — 국세청 HTTP 호출을 트랜잭션 밖에 두고 DB 작업만 writer 의
     * 짧은 트랜잭션으로 위임한다. 일일 시도 한도·감사 로그도 선행 인증과 같은 집계를 공유한다.
     */
    public CompanyReverificationResponse reverify(Long userId, Long companyId, CompanyReverificationRequest request) {
        // 사업자번호는 요청이 아니라 DB 저장값을 쓴다(탈취 방지). 소유·선행조건(businessVerified)도 여기서 확인한다.
        String businessNumber = writer.loadOwnedVerifiedBusinessNumber(companyId, userId);

        LocalDateTime now = LocalDateTime.now();
        long used = writer.countTodayAttempts(userId, now.toLocalDate().atStartOfDay());
        if (used >= DAILY_ATTEMPT_LIMIT) {
            throw new BusinessException(CompanyErrorCode.VERIFICATION_LIMIT_EXCEEDED);
        }

        NtsClient.Result result;
        try {
            result = ntsClient.verify(businessNumber, request.ceoName(), request.businessStartDate());
        } catch (NtsClient.UnavailableException e) {
            writer.recordAttempt(userId, businessNumber, request.ceoName(), request.businessStartDate(),
                    VerificationOutcome.UNAVAILABLE, null);
            throw new BusinessException(CompanyErrorCode.VERIFICATION_UNAVAILABLE);
        }

        if (!result.isValid()) {
            writer.recordAttempt(userId, businessNumber, request.ceoName(), request.businessStartDate(),
                    result.outcome(), result.rawResponse());
            throw new BusinessException(toErrorCode(result.outcome()));
        }

        LocalDateTime verifiedAt = writer.applyReverification(userId, companyId, businessNumber,
                request.ceoName(), request.businessStartDate(), result.rawResponse(), now);

        return CompanyReverificationResponse.of(businessNumber, request.ceoName(), request.businessStartDate(), verifiedAt);
    }

    // 판정별로 다른 안내를 준다. 사용자가 고쳐야 할 것이 각각 다르기 때문이다 —
    // 불일치는 입력을 고치면 되지만, 미등록·폐업은 고칠 수 있는 입력이 없다.
    private CompanyErrorCode toErrorCode(VerificationOutcome outcome) {
        return switch (outcome) {
            case MISMATCH -> CompanyErrorCode.VERIFICATION_MISMATCH;
            case NOT_REGISTERED -> CompanyErrorCode.VERIFICATION_NOT_REGISTERED;
            case SUSPENDED -> CompanyErrorCode.VERIFICATION_SUSPENDED;
            case CLOSED -> CompanyErrorCode.VERIFICATION_CLOSED;
            // NtsClient 는 이 셋을 반환하지 않는다(VALID 는 위에서 걸러지고, 나머지 둘은 서비스가 붙이는 값).
            case VALID, UNAVAILABLE, DUPLICATE -> throw new IllegalStateException("국세청 판정이 아닌 값: " + outcome);
        };
    }
}
