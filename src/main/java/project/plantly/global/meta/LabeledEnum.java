package project.plantly.global.meta;

/**
 * 표시 문구를 <b>서버가 소유하는</b> enum 임을 나타낸다. 이 인터페이스를 구현한 값만 옵션 카탈로그
 * ({@link OptionCatalog})에 실릴 수 있다.
 *
 * <p>인터페이스를 두는 이유는 리플렉션으로 {@code getLabel()} 을 찾아 쓰지 않기 위해서다. 그렇게 하면
 * 카탈로그가 "메서드 이름이 우연히 맞는 아무 enum"이나 열어보는 창구가 되고, 라벨 없는 enum 을 등록해도
 * 런타임에야 터진다. 구현을 강제하면 "이 값의 문구는 API 계약의 일부다" 가 타입으로 남는다.
 *
 * <p><b>모든 enum 이 이걸 구현해야 하는 것은 아니다.</b> 라벨의 주인은 값마다 다르게 정해져 있고,
 * 판단 기준은 {@link OptionCatalog} 주석에 한 곳으로 모아 뒀다(싣는 기준 셋 · 안 싣는 케이스 넷).
 * <b>여기에 기준을 다시 적지 않는다</b> — 두 곳에 적어두면 한쪽만 보고 판단하게 되고, 실제로 그렇게
 * {@code CompanyVisibility} 를 잘못 넣은 적이 있다.
 *
 * <p>도메인 enum 이 global 패키지의 타입을 구현하는 것이 어색해 보일 수 있지만, 방향은 맞다 —
 * "이 문구가 밖으로 나간다" 는 사실은 도메인 규칙이 아니라 API 계약이고, 그 계약을 여기서 소유한다.
 */
public interface LabeledEnum {

    /** 화면에 그대로 노출되는 한글 표기. 비어 있으면 안 된다({@code OptionCatalogTest} 가 지킨다). */
    String getLabel();

    /** enum 상수 이름. 요청·응답에 오가는 값이다(구현체가 enum 이므로 자동으로 채워진다). */
    String name();
}
