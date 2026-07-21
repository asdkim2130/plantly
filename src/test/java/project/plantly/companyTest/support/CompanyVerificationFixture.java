package project.plantly.companyTest.support;

import org.springframework.test.util.ReflectionTestUtils;
import project.plantly.domain.company.entity.CompanyVerification;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 선행 인증 레코드 픽스처.
 *
 * <p>실제 발급 경로는 국세청 호출을 거치지만, 등록 오케스트레이션 테스트는 "이미 인증을 통과한 상태"에서
 * 시작하면 되므로 팩토리로 직접 만든다. id 는 리포지토리가 채우는 값이라 리플렉션으로 심는다.
 */
public class CompanyVerificationFixture {

    public static final String BUSINESS_NUMBER = "1234567890";
    public static final String CEO_NAME = "홍길동";
    public static final LocalDate START_DATE = LocalDate.of(2020, 1, 2);

    /** 사용 가능한(미사용·미만료) 인증. */
    public static CompanyVerification usable(Long id, Long userId) {
        CompanyVerification verification = CompanyVerification.issue(
                userId, BUSINESS_NUMBER, CEO_NAME, START_DATE, LocalDateTime.now(), Duration.ofMinutes(30));
        ReflectionTestUtils.setField(verification, "id", id);
        return verification;
    }

    /** 유효기간이 이미 지난 인증. */
    public static CompanyVerification expired(Long id, Long userId) {
        CompanyVerification verification = CompanyVerification.issue(
                userId, BUSINESS_NUMBER, CEO_NAME, START_DATE,
                LocalDateTime.now().minusHours(2), Duration.ofMinutes(30));
        ReflectionTestUtils.setField(verification, "id", id);
        return verification;
    }

    /** 이미 다른 회사 등록에 쓰인 인증. */
    public static CompanyVerification consumed(Long id, Long userId, Long companyId) {
        CompanyVerification verification = usable(id, userId);
        verification.consume(companyId);
        return verification;
    }
}
