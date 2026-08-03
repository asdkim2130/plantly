package project.plantly.global.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 프론트 개발용 fake data 시드 설정. {@code seed} 프로파일에서만 읽힌다.
 *
 * <p>{@code reset=true} 는 회사 도메인 테이블을 전부 비우고 처음부터 다시 심는다. false(기본)면
 * 완료 표식({@link SeedState})이 있을 때 아무것도 하지 않는다 — 재기동으로 데이터가 불어나지 않게 하는
 * 멱등 스위치. 표식이 없는데 시드 흔적만 남아 있으면(중간에 깨진 시드) 러너가 멈추고 리셋을 요구한다.
 */
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(boolean reset, String outputDir) {

    // 케이스↔id 매핑 산출물이 떨어지는 곳. 프론트가 열어보기 쉽도록 리포 경로에 쓰되, 시드 실행 결과일
    // 뿐이라 커밋하지는 않는다(.gitignore 의 /docs/seed/).
    private static final String DEFAULT_OUTPUT_DIR = "docs/seed";

    public SeedProperties {
        if (outputDir == null || outputDir.isBlank()) {
            outputDir = DEFAULT_OUTPUT_DIR;
        }
    }
}
