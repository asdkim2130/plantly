package project.plantly.global.seed;

import java.time.LocalDate;
import java.util.List;

/**
 * 한국어 제조업 어휘 조합기.
 *
 * <p>datafaker 같은 라이브러리를 쓰지 않는 이유는 두 가지다. (1) 시드 코드가 main 소스셋에 있어
 * 의존성이 프로덕션 jar 에 그대로 실린다. (2) ko 로케일의 회사명 사전은 제조업 디렉토리에 어울리지
 * 않는다. 어근·업종어 리스트 두 개면 500 조합이 나오고, 이 프로젝트에 필요한 수십 건에는 충분하다.
 *
 * <p>모든 함수는 순번(index)만 받는 순수 함수다 — 난수가 없으므로 같은 순서로 심으면 같은 값이 나온다.
 */
public final class SeedVocabulary {

    private static final List<String> STEMS = List.of(
            "대성", "우진", "한신", "삼정", "동방", "신흥", "태광", "유성", "세아", "금호",
            "두원", "백산", "청우", "명진", "성일", "광명", "한독", "서일", "남광", "정우",
            "미래", "태평", "강산", "예성", "한울");

    private static final List<String> TRADES = List.of(
            "정밀", "테크", "산업", "엔지니어링", "기공", "열처리", "금속", "화학", "소재", "전자",
            "기계", "특수강", "몰드", "프레스", "도장", "용접", "주조", "단조", "표면처리", "자동화");

    private static final List<String> SURNAMES = List.of("김", "이", "박", "최", "정", "강", "조", "윤", "장", "임");

    private static final List<String> GIVEN_NAMES = List.of(
            "대현", "성호", "진우", "영수", "미경", "정숙", "태영", "상철", "현주", "경민",
            "동훈", "재석", "수민", "지훈", "혜영");

    private static final List<String> STREETS = List.of(
            "산업로", "테크노로", "공단로", "첨단로", "중앙로", "디지털로", "과학로", "성장로");

    // 시군구 자식이 없는 광역시·특별시(대응 지역은 광역시를 한 단위로 본다)의 주소에 붙일 구 이름.
    // 도로명·번지가 이미 가짜인 데이터라 지리적 정확성이 아니라 "시도 + 시군구" 형태를 맞추는 게 목적이다
    // — 카드의 지역 라벨이 시도만으로 끝나면 도로명 조각을 물기 때문. 조합에 따라 실재하지 않는 구가 나올 수 있다.
    private static final List<String> DISTRICTS = List.of("중구", "동구", "북구", "남구", "서구");

    private static final List<String> MATERIALS = List.of(
            "알루미늄", "스테인리스", "티타늄", "황동", "탄소강", "엔지니어링 플라스틱", "인코넬", "마그네슘");

    private static final List<String> EQUIPMENT = List.of(
            "5축 머시닝센터", "CNC 선반", "와이어 방전기", "레이저 커팅기",
            "사출성형기", "산업용 3D 프린터", "유압 프레스", "진공 열처리로");

    private static final List<String> TAGS = List.of(
            "소량다품종", "시제품제작", "양산대응", "정밀가공", "신속납기", "금형설계", "표면처리", "공정자동화");

    private static final List<String> LEAD_TIMES = List.of(
            "3~5 영업일", "2주 내외", "4주 내외", "수량 협의 후 결정", "재고 보유 시 당일 출고");

    private static final List<String> AS_INFOS = List.of(
            "출고 후 1년 무상 보증. 소모품은 별도 협의합니다.",
            "설치 후 2년간 정기 점검을 무상 제공합니다.",
            "불량 발생 시 왕복 배송비 포함 무상 재가공해 드립니다.",
            "원격 기술지원 상시 운영, 현장 출동은 영업일 기준 2일 이내입니다.");

    private static final List<String> BRAND_COLORS = List.of(
            "#1F6FEB", "#D97706", "#059669", "#7C3AED", "#DC2626", "#0891B2");

    private SeedVocabulary() {
    }

    /** 회사명. 어근×업종어로 index 500 까지 중복 없이 나온다. */
    public static String companyName(int index) {
        String stem = STEMS.get(Math.floorMod(index, STEMS.size()));
        String trade = TRADES.get(Math.floorMod(index / STEMS.size(), TRADES.size()));
        return stem + trade;
    }

    public static String ceoName(int index) {
        return SURNAMES.get(Math.floorMod(index, SURNAMES.size()))
                + GIVEN_NAMES.get(Math.floorMod(index / SURNAMES.size(), GIVEN_NAMES.size()));
    }

    public static String contactName(int index) {
        return ceoName(index + 37);
    }

    /** 광역시·특별시 주소에 붙일 구 이름. {@link SeedMasterCatalog#addressRegionName} 이 쓴다. */
    public static String district(int index) {
        return DISTRICTS.get(Math.floorMod(index, DISTRICTS.size()));
    }

    /**
     * 도로명 주소. 앞에 붙는 지역 명칭은 {@link SeedMasterCatalog#addressRegionName} 이 "시도 시군구" 형태로
     * 맞춰 넘겨준다 — 주소 문자열과 지역 필터가 어긋나지 않게 하면서, 카드 지역 라벨도 온전하게 잘리도록.
     */
    public static String roadAddress(String regionName, int index) {
        String street = STREETS.get(Math.floorMod(index, STREETS.size()));
        int number = 10 + Math.floorMod(index * 17, 400);
        return regionName + " " + street + " " + number;
    }

    public static String jibunAddress(String regionName, int index) {
        int bunji = 100 + Math.floorMod(index * 23, 800);
        return regionName + " " + bunji + "-" + (1 + Math.floorMod(index, 9));
    }

    public static String detailAddress(int index) {
        return (1 + Math.floorMod(index, 9)) + "동 " + (100 + Math.floorMod(index * 7, 800)) + "호";
    }

    /**
     * 설립일(=자가등록 회사의 개업일자). 인증 레코드와 회사 본체가 같은 값을 갖도록 한 곳에서만 만든다 —
     * 자가등록은 개업일자를 요청으로 받지 않고 인증에서 가져오므로, 두 곳에서 따로 계산하면 어긋난다.
     */
    public static LocalDate establishmentDate(int index) {
        return LocalDate.of(
                1998 + Math.floorMod(index, 25),
                1 + Math.floorMod(index, 12),
                1 + Math.floorMod(index * 3, 28));
    }

    public static String postalCode(int index) {
        return String.format("%05d", 10000 + Math.floorMod(index * 137, 89000));
    }

    public static String phone(int index) {
        return String.format("031-%03d-%04d", 200 + Math.floorMod(index * 3, 700), Math.floorMod(index * 137, 10000));
    }

    public static String email(int index) {
        return "contact" + String.format("%03d", index) + "@plantly.local";
    }

    public static String website(int index) {
        return "https://www.plantly-demo.local/company/" + String.format("%03d", index);
    }

    /** 결정론적 placeholder 이미지. 같은 index 는 항상 같은 그림이라 스크린샷 비교가 가능하다. */
    public static String logoUrl(int index) {
        return "https://picsum.photos/seed/plantly-logo-" + index + "/200/200";
    }

    public static String imageUrl(int index, int slot) {
        return "https://picsum.photos/seed/plantly-" + index + "-" + slot + "/800/600";
    }

    /**
     * 카드 커버. 로고와 다른 그림이어야 두 자리가 같은 이미지로 채워지는 실수가 화면에서 바로 드러나므로
     * seed 접두어를 분리한다. 16:9 로 뽑는 이유는 커버가 스팟라이트(거의 정사각)와 추천 카드(가로로 긴 띠)
     * 양쪽에서 서로 다른 비율로 잘리기 때문이다 — 한 장으로 두 크롭을 감당하는지 시드로 확인된다.
     */
    public static String coverImageUrl(int index) {
        return "https://picsum.photos/seed/plantly-cover-" + index + "/1200/675";
    }

    public static String videoUrl(int index) {
        return "https://www.youtube.com/watch?v=plantly" + String.format("%03d", index);
    }

    public static String introTitle(int index) {
        String trade = TRADES.get(Math.floorMod(index / STEMS.size(), TRADES.size()));
        int years = 8 + Math.floorMod(index * 3, 25);
        return trade + " 분야 " + years + "년, 시제품부터 양산까지 한 곳에서";
    }

    public static String content(int index) {
        String name = companyName(index);
        String trade = TRADES.get(Math.floorMod(index / STEMS.size(), TRADES.size()));
        String material = MATERIALS.get(Math.floorMod(index, MATERIALS.size()));
        String equipment = EQUIPMENT.get(Math.floorMod(index + 3, EQUIPMENT.size()));
        return """
                %s은(는) %s 전문 기업으로, %s 소재 가공에 강점을 가지고 있습니다.
                %s를 포함한 설비를 갖추어 시제품 제작부터 양산까지 대응합니다.
                도면 검토 단계에서 가공성 개선 의견을 함께 드리며, 초도 물량은 전수 검사로 납품합니다."""
                .formatted(name, trade, material, equipment);
    }

    public static String leadTime(int index) {
        return LEAD_TIMES.get(Math.floorMod(index, LEAD_TIMES.size()));
    }

    public static String asInfo(int index) {
        return AS_INFOS.get(Math.floorMod(index, AS_INFOS.size()));
    }

    public static String brandColor(int index) {
        return BRAND_COLORS.get(Math.floorMod(index, BRAND_COLORS.size()));
    }

    public static List<String> materials(int index, int count) {
        return pick(MATERIALS, index, count);
    }

    public static List<String> equipment(int index, int count) {
        return pick(EQUIPMENT, index, count);
    }

    public static List<String> tags(int index, int count) {
        return pick(TAGS, index, count);
    }

    public static String projectTitle(int index) {
        String material = MATERIALS.get(Math.floorMod(index, MATERIALS.size()));
        return material + " 부품 양산 라인 구축";
    }

    public static String projectAchievements(int index) {
        int rate = 12 + Math.floorMod(index * 5, 30);
        return "공정 재설계로 사이클 타임 " + rate + "% 단축, 초기 불량률 0.3% 이하 유지";
    }

    public static String projectPartners(int index) {
        return companyName(index + 11) + ", " + companyName(index + 23);
    }

    public static String projectPeriod(int index) {
        int startYear = 2019 + Math.floorMod(index, 5);
        return startYear + ".03 ~ " + (startYear + 1) + ".08";
    }

    private static List<String> pick(List<String> source, int index, int count) {
        int take = Math.min(count, source.size());
        return java.util.stream.IntStream.range(0, take)
                .mapToObj(i -> source.get(Math.floorMod(index + i, source.size())))
                .toList();
    }
}
