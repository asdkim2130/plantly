package project.plantly.domain.company.nts;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 국세청 사업자등록정보 진위확인/상태조회 API 설정.
 *
 * <p>serviceKey 는 시크릿이라 커밋된 기본값 없이 환경변수(NTS_SERVICE_KEY)로만 주입한다.
 * fake=true 면 외부 호출 없이 {@link FakeNtsClient} 가 뜬다 — 로컬/테스트 전용.
 *
 * <p>fake 를 "키가 비어있으면 자동"으로 켜지 않는 이유: 운영에서 환경변수 주입이 실패하면
 * 조용히 가짜 클라이언트가 떠서 아무 사업자번호나 인증을 통과시킨다. 명시적 플래그로만 켜고,
 * fake=false 인데 키가 없으면 {@link NtsConfig} 가 부팅을 실패시킨다.
 */
@ConfigurationProperties(prefix = "app.nts")
public record NtsProperties(
        String serviceKey,
        String baseUrl,
        boolean fake,
        Duration connectTimeout,
        Duration readTimeout
) {

    private static final String DEFAULT_BASE_URL = "https://api.odcloud.kr/api/nts-businessman/v1";

    public NtsProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = DEFAULT_BASE_URL;
        // 국세청 API 가 느려질 때 요청 스레드가 무한정 묶이지 않도록 짧게 잡는다.
        // 등록 흐름 앞단(선행 인증)이라 사용자를 오래 기다리게 하느니 실패시키고 재시도를 안내하는 편이 낫다.
        if (connectTimeout == null) connectTimeout = Duration.ofSeconds(3);
        if (readTimeout == null) readTimeout = Duration.ofSeconds(5);
    }
}
