package project.plantly.global.meta;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.global.meta.dto.EnumOption;
import project.plantly.global.response.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MetaOptionsController {

    /**
     * 고정 선택지 카탈로그. 서버 enum 하나가 응답의 키 하나이고, 값은 그 enum 의 선택지 목록이다.
     *
     * <p>제약 조회({@link MetaConstraintsController})와 나눠 두는 이유는 쓰임이 다르기 때문이다.
     * 제약은 "이 폼의 이 칸을 어떻게 막을 것인가" 라 폼 단위로 묻지만, 선택지는 폼 밖에서도 필요하다 —
     * 공개 상세 화면은 {@code trlLevel: "MASS_PRODUCTION"} 을 받아 "양산 적용 가능" 으로 그려야 하는데
     * 거기엔 폼이 없다. 그래서 폼에 매이지 않은 자리에 둔다.
     *
     * <p>같은 이유로 <b>비로그인도 읽을 수 있다</b>(SecurityConfig 에서 permitAll). 공개 상세는 익명에게도
     * 열려 있으므로, 여기 인증을 걸면 로그인한 사람에게만 라벨이 보이는 화면이 된다.
     *
     * <p>낱개 조회 경로는 두지 않는다. 전부 합쳐도 수백 바이트라 폼이 열릴 때 한 번 받아두는 편이
     * 칸마다 왕복하는 것보다 단순하고, 사실상 불변이라 캐시도 잘 든다.
     *
     * <p>여기 실리는 enum 은 {@link OptionCatalog} 에 적힌 것뿐이고, 무엇을 싣고 무엇을 빼는지의 기준은
     * 그 클래스 주석에 있다 — 모든 enum 의 문구를 서버가 소유하는 것은 아니다.
     */
    @GetMapping("/api/v1/meta/options")
    public ApiResponse<Map<String, List<EnumOption>>> getOptions() {

        return ApiResponse.success(OptionCatalog.all());
    }
}
