package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.plantly.domain.company.dto.CompanyVerificationRequest;
import project.plantly.domain.company.dto.CompanyVerificationResponse;
import project.plantly.domain.company.entity.CompanyVerification;
import project.plantly.domain.company.enums.VerificationOutcome;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.nts.NtsClient;
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

        return CompanyVerificationResponse.from(verification);
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
