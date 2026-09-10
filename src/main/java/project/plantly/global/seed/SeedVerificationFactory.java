package project.plantly.global.seed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.dto.CompanyVerificationRequest;
import project.plantly.domain.company.dto.MyCompanyCreateRequest;
import project.plantly.domain.company.entity.CompanyDraft;
import project.plantly.domain.company.entity.CompanyVerification;
import project.plantly.domain.company.repository.CompanyDraftRepository;
import project.plantly.domain.company.repository.CompanyVerificationRepository;
import project.plantly.domain.company.service.CompanyVerificationService;
import project.plantly.global.exception.BusinessException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 선행 인증·임시저장 초안 생성.
 *
 * <p><b>발급 경로가 두 갈래인 이유</b> — {@link CompanyVerificationService} 는 유저당 하루 5회
 * ({@code DAILY_ATTEMPT_LIMIT}) 로 제한되고 발급된 인증의 수명이 30분({@code VERIFICATION_TTL})이다.
 * 둘 다 {@code private static final} 이라 설정으로 조정할 수 없다. 시드는 한 계정에 인증을 여러 건
 * 만들어야 하고, 초안 케이스는 오전에 심어 오후에 열어도 살아 있어야 하므로 그 경로로는 불가능하다.
 *
 * <ul>
 *   <li>{@link #issue} — 엔티티 팩토리 직접 호출. TTL 을 지정할 수 있어 장수명·만료 인증을 자유롭게 만든다.
 *       국세청 호출도 하루 한도도 타지 않는다.</li>
 *   <li>{@link #recordFailedAttempt} — 실제 서비스 경로. {@code FakeNtsClient} 가 끝 4자리로 실패를
 *       분기하므로, 프론트가 인증 실패 화면을 실제 응답으로 볼 수 있게 감사 로그를 남긴다.</li>
 * </ul>
 *
 * <p>두 경로 모두 외부 호출은 없다 — 러너가 시작 전에 가짜 클라이언트인지 확인하고 아니면 중단한다.
 */
@Slf4j
@Component
@Profile(SeedConfig.PROFILE)
@RequiredArgsConstructor
public class SeedVerificationFactory {

    /** 초안 케이스가 시드 다음 날에도 살아 있도록 넉넉히 잡는다. */
    private static final Duration LONG_TTL = Duration.ofDays(365);

    private final CompanyVerificationRepository verificationRepository;
    private final CompanyDraftRepository draftRepository;
    private final CompanyVerificationService verificationService;
    private final ObjectMapper objectMapper;

    /** 사용 가능한 인증. 회사 등록에 소비되거나, 초안을 매달 수 있다. */
    @Transactional
    public Long issue(Long userId, String businessNumber, String ceoName, LocalDate businessStartDate) {
        CompanyVerification verification = CompanyVerification.issue(
                userId, businessNumber, ceoName, businessStartDate, LocalDateTime.now(), LONG_TTL);
        return verificationRepository.save(verification).getId();
    }

    /**
     * 이미 만료된 인증. 발급 시각을 과거로 두고 짧은 TTL 을 적용해, 서비스가 만료를 읽는 시점에
     * EXPIRED 로 전이시키도록 둔다(상태를 미리 박아두지 않는 편이 실제 흐름과 같다).
     *
     * <p>TTL 을 설정값({@code app.verification.ttl})이 아니라 여기 고정값으로 두는 이유는, 이 팩토리의
     * 목적이 "만료된 상태를 만드는 것" 이라서다. 설정을 따라가면 기본값(7일)이 늘어날 때마다 발급 시각을
     * 함께 밀어야 하고, 그러다 한 번 어긋나면 만료 케이스가 조용히 유효한 인증이 된다.
     */
    @Transactional
    public Long issueExpired(Long userId, String businessNumber, String ceoName, LocalDate businessStartDate) {
        CompanyVerification verification = CompanyVerification.issue(
                userId, businessNumber, ceoName, businessStartDate,
                LocalDateTime.now().minusDays(2), Duration.ofMinutes(30));
        return verificationRepository.save(verification).getId();
    }

    /**
     * 임시저장 초안. payload 는 발행 요청 DTO 를 그대로 직렬화한다 — 손으로 JSON 을 쓰면 DTO 변경에 조용히 어긋난다.
     *
     * <p>인자로는 인증 id 를 받지만 초안이 매달리는 키는 그 인증의 <b>사업자번호</b>다({@link CompanyDraft} 주석).
     * 시드 케이스가 "이 인증에 딸린 초안"으로 읽히는 편이 자연스러워 호출부 모양은 그대로 두고 여기서 옮긴다.
     */
    @Transactional
    public void saveDraft(Long verificationId, Long userId, MyCompanyCreateRequest payload) {
        CompanyVerification verification = verificationRepository.findById(verificationId).orElseThrow();
        draftRepository.save(
                CompanyDraft.create(userId, verification.getBusinessNumber(), serialize(payload)));
    }

    /**
     * 인증 실패 감사 로그. 실제 서비스 경로를 태워 {@code CompanyVerificationAttempt} 가 남게 한다.
     * 실패가 목적이므로 예외는 삼킨다 — 통과해 버리면 오히려 케이스가 만들어지지 않은 것이다.
     */
    public void recordFailedAttempt(Long userId, String businessNumber, String ceoName, LocalDate businessStartDate) {
        try {
            verificationService.verify(userId, new CompanyVerificationRequest(businessNumber, ceoName, businessStartDate));
            log.warn("[seed] 실패 케이스로 넣은 사업자번호({})가 인증을 통과했습니다. FakeNtsClient 분기 규칙을 확인하세요.",
                    businessNumber);
        } catch (BusinessException expected) {
            log.debug("[seed] 인증 실패 감사 로그 생성: {} -> {}", businessNumber, expected.getMessage());
        }
    }

    private String serialize(MyCompanyCreateRequest payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("초안 payload 직렬화에 실패했습니다.", e);
        }
    }
}
