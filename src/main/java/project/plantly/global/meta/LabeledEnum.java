package project.plantly.global.meta;

/**
 * 표시 문구를 <b>서버가 소유하는</b> enum 임을 나타낸다. 이 인터페이스를 구현한 값만 옵션 카탈로그
 * ({@link OptionCatalog})에 실릴 수 있다.
 *
 * <p>인터페이스를 두는 이유는 리플렉션으로 {@code getLabel()} 을 찾아 쓰지 않기 위해서다. 그렇게 하면
 * 카탈로그가 "메서드 이름이 우연히 맞는 아무 enum"이나 열어보는 창구가 되고, 라벨 없는 enum 을 등록해도
 * 런타임에야 터진다. 구현을 강제하면 "이 값의 문구는 API 계약의 일부다" 가 타입으로 남는다.
 *
 * <p><b>모든 enum 이 이걸 구현해야 하는 것은 아니다.</b> 라벨의 주인은 값마다 다르게 정해져 있다 —
 * 사용자가 <i>고르는</i> 값의 문구는 서버가 소유하지만(고른 값이 저장되고 다시 표시되므로 한 곳에서
 * 나와야 한다), 그룹핑·배지처럼 화면이 만들어내는 표현 장치는 프론트가 소유한다
 * ({@code CertificationPublicResponse}/{@code CountryPublicResponse} 주석 참고 — {@code type}·
 * {@code continent} 는 일부러 라벨 없이 값만 내보낸다).
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
