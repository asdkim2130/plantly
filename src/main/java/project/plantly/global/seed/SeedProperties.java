package project.plantly.global.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 프론트 개발용 fake data 시드 설정. {@code seed} 프로파일에서만 읽힌다.
 *
 * <p>{@code reset=true} 는 회사 도메인 테이블을 전부 비우고 처음부터 다시 심는다. false(기본)면
 * 마커 회사가 이미 있을 때 아무것도 하지 않는다 — 재기동으로 데이터가 불어나지 않게 하는 멱등 스위치.
 */
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(boolean reset, String outputDir) {

    // 케이스↔id 매핑 산출물이 떨어지는 곳. 프론트가 계속 참조하므로 리포 안에 둔다.
    private static final String DEFAULT_OUTPUT_DIR = "docs/seed";

    public SeedProperties {
        if (outputDir == null || outputDir.isBlank()) {
            outputDir = DEFAULT_OUTPUT_DIR;
        }
    }
}
