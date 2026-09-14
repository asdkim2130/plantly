package project.plantly.domain.company.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 선행 사업자 인증의 수명·시도 한도 정책.
 *
 * <p>상수가 아니라 설정값인 이유는 두 값 모두 "국세청 쿼터를 얼마나 쓸 것인가" 의 운영 판단이기 때문이다.
 * 쿼터 사정은 배포마다 다르고, 코드를 고치지 않고 조일 수 있어야 한다.
 *
 * <p><b>ttl 이 짧을수록 안전해지는 것이 아니다.</b> 이 값이 방어하는 것은 "인증만 받아두고 방치한 사이
 * 사업자 상태가 바뀌는 것"(폐업 등)인데, 폐업은 분 단위로 일어나는 사건이 아니라 짧게 잡아도 사실상
 * 아무것도 더 막지 못한다. 반대로 짧으면 정상 사용자가 폼을 작성하는 도중에 만료를 만나고, 그때마다
 * 발행 경로가 국세청에 재질의하게 되어 쿼터만 두 배로 쓴다. 그래서 며칠 단위로 잡는다 —
 * 그 사이 상태가 바뀌었을 가능성은 발행 시점의 자동 재질의
 * ({@code CompanyVerificationService#refreshIfExpired})가 대신 막는다.
 *
 * <p>dailyAttemptLimit 은 <b>사용자가 직접 누른 인증 시도</b>만 센다. 발행 시점의 자동 재질의는 사용자가
 * 인지할 수 없는 호출이라 한도에서 빠진다 — 그걸 세면 "가만히 있었는데 한도를 다 썼다" 가 성립한다
 * ({@code CompanyVerificationAttempt#automatic}).
 */
@ConfigurationProperties(prefix = "app.verification")
public record VerificationProperties(
        Duration ttl,
        Integer dailyAttemptLimit
) {

    private static final Duration DEFAULT_TTL = Duration.ofDays(7);

    // 국세청 API 일일 쿼터 보호 + 대표자명을 바꿔가며 찔러보는 시도 차단.
    private static final int DEFAULT_DAILY_ATTEMPT_LIMIT = 5;

    public VerificationProperties {
        if (ttl == null) ttl = DEFAULT_TTL;
        if (dailyAttemptLimit == null) dailyAttemptLimit = DEFAULT_DAILY_ATTEMPT_LIMIT;
    }
}
