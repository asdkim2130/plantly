package project.plantly.domain.company.nts;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * 국세청 클라이언트 배선. fake 플래그에 따라 실제 구현과 가짜 구현 중 하나만 빈으로 올린다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(NtsProperties.class)
public class NtsConfig {

    @Bean
    public NtsClient ntsClient(NtsProperties properties, ObjectMapper objectMapper) {
        if (properties.fake()) {
            log.warn("국세청 인증이 가짜 클라이언트로 동작합니다 (app.nts.fake=true). 운영 환경에서는 절대 켜지 마세요.");
            return new FakeNtsClient();
        }

        // 여기가 운영 안전장치다. 환경변수 주입이 실패한 채로 서비스가 뜨면 모든 인증 요청이
        // 런타임에 실패하거나(키 오류) 최악의 경우 조용히 잘못 동작한다. 부팅 시점에 즉시 죽이는 편이 낫다.
        if (properties.serviceKey() == null || properties.serviceKey().isBlank()) {
            throw new IllegalStateException(
                    "국세청 API 서비스 키가 설정되지 않았습니다. 환경변수 NTS_SERVICE_KEY 를 주입하거나, "
                            + "로컬/테스트라면 app.nts.fake=true 로 가짜 클라이언트를 사용하세요.");
        }

        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(properties.connectTimeout())
                .withReadTimeout(properties.readTimeout());

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();

        return new NtsRestClient(restClient, objectMapper, properties.serviceKey());
    }
}
