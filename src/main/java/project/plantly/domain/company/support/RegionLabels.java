package project.plantly.domain.company.support;

import java.util.Set;

/**
 * 도로명 주소 → 카드 표기용 지역 라벨.
 *
 * <p>목록 카드는 전체 주소가 아니라 "어느 지역 회사인가"만 보이면 되므로 시도 + 시군구까지만 남긴다.
 * 카카오(다음) 우편번호 서비스가 내려주는 도로명 주소는 시도·시군구를 앞쪽 토큰으로 두므로
 * ("서울시 강남구 테헤란로 1" → "서울시 강남구", "경기 화성시 동탄대로 45" → "경기 화성시")
 * 앞 2토큰이 그대로 라벨이 된다. 일반시의 구도 3번째 토큰이라 자연히 잘린다
 * ("경기 성남시 분당구 …" → "경기 성남시").
 *
 * <p>예외는 시군구 단계가 없는 시도 하나뿐이다 — 세종은 2토큰을 자르면 도로명이 딸려오므로
 * ("세종특별자치시 한누리대로 2130") 1토큰만 쓴다. {@link #NO_SIGUNGU_SIDO} 참고.
 *
 * <p>파생 결과를 컬럼에 저장하지 않고 읽기 시점에 만든다 — 표기 규칙일 뿐이라 원본(road_address)과
 * 어긋날 여지를 두지 않고, 컬럼 추가·백필도 필요 없다. 지역으로 DB 필터/정렬/집계를 해야 하는 날이
 * 오면 그때 이 함수로 컬럼을 백필한다(규칙이 여기 한 곳에 있으므로 이전 비용이 낮다).
 */
public final class RegionLabels {

    // 시군구 단계가 없어 1토큰만 취하는 시도. 세종 하나이며, 표기 흔들림에 대비해 축약형까지 함께 둔다.
    private static final Set<String> NO_SIGUNGU_SIDO = Set.of("세종특별자치시", "세종시", "세종");

    private RegionLabels() {
    }

    /**
     * 도로명 주소에서 "시도 시군구"만 남긴 라벨을 돌려준다.
     * null·공백이거나 토큰이 하나뿐이면(=자를 게 없으면) 입력을 그대로 돌려준다.
     */
    public static String fromRoadAddress(String roadAddress) {
        if (roadAddress == null || roadAddress.isBlank()) {
            return roadAddress;
        }
        String[] tokens = roadAddress.trim().split("\\s+");
        if (tokens.length == 1 || NO_SIGUNGU_SIDO.contains(tokens[0])) {
            return tokens[0];
        }
        return tokens[0] + " " + tokens[1];
    }
}
