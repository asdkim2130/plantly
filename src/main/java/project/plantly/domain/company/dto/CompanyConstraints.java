package project.plantly.domain.company.dto;

/**
 * 회사 입력 제약의 단일 출처. 등록(관리자/자가) · 수정 세 경로의 DTO 가 같은 값을 참조한다.
 *
 * <p>여기 모으는 이유는 경로마다 제약이 갈리면 등록에서 막은 값이 수정으로 들어오기 때문이다.
 * 애너테이션 속성은 컴파일 타임 상수만 받으므로 상수로 둔다.
 *
 * <p>세 종류가 섞여 있으니 구분해서 읽어야 한다:
 * <ul>
 *   <li><b>길이</b> — 대부분 엔티티 컬럼이 {@code varchar(255)}(ddl-auto 기본값)라는 사실에서 온다.
 *       이 상한이 없으면 초과 입력이 검증이 아니라 영속화 시점에 터진다.</li>
 *   <li><b>개수</b> — 화면이 감당할 양. 등급과 무관한 절대 천장이다.</li>
 *   <li><b>등급 천장</b> — 아래 CEILING 셋. 등급별 실제 한도는 GradePolicy 가 소유하고,
 *       이 값은 그중 최고 등급값이다. 자세한 것은 각 상수 주석 참고.</li>
 * </ul>
 */
public final class CompanyConstraints {

    private CompanyConstraints() {
    }

    // ===== 형식 =====

    /** 사업자등록번호. 국세청 인증 경로(CompanyVerificationRequest)와 같은 규칙을 쓴다. */
    public static final String BUSINESS_NUMBER_PATTERN = "\\d{10}";
    public static final String BUSINESS_NUMBER_MESSAGE = "사업자등록번호는 숫자 10자리여야 합니다.";

    /** 우편번호(국가기초구역번호). */
    public static final String POSTAL_CODE_PATTERN = "\\d{5}";
    public static final String POSTAL_CODE_MESSAGE = "우편번호는 5자리 숫자여야 합니다.";

    /** 연락처. 국제번호·내선을 아직 받지 않으므로 숫자와 하이픈만 허용한다. */
    public static final String PHONE_PATTERN = "[0-9-]+";
    public static final String PHONE_MESSAGE = "연락처는 숫자와 하이픈만 입력할 수 있습니다.";

    // 브랜드 컬러 형식(#RRGGBB). 화면이 이 값을 CSS 색상으로 그대로 사용하므로(메인 스팟라이트 배너 배경 등)
    // 임의 문자열이 들어가면 안 된다. 대소문자 헥사를 모두 받되 축약형(#RGB)·색 이름·rgb() 표기는 받지 않는다 —
    // 저장 표기를 하나로 고정해 프론트가 명도 계산(텍스트 대비 반전)을 분기 없이 할 수 있게 한다.
    public static final String BRAND_COLOR_PATTERN = "#[0-9a-fA-F]{6}";

    // 수정 경로 전용: 빈 문자열을 추가로 허용한다("" = 색 비우기, Company.updateBasicInfo 의 clear 규약).
    public static final String BRAND_COLOR_CLEARABLE_PATTERN = "|" + BRAND_COLOR_PATTERN;

    public static final String BRAND_COLOR_MESSAGE = "브랜드 컬러는 #RRGGBB 형식이어야 합니다.";

    // ===== 길이 =====

    public static final int COMPANY_NAME_MAX = 100;
    public static final int CEO_NAME_MAX = 50;
    public static final int ROAD_ADDRESS_MAX = 200;
    public static final int JIBUN_ADDRESS_MAX = 200;
    public static final int DETAIL_ADDRESS_MAX = 100;
    public static final int INTRO_TITLE_MAX = 50;
    public static final int LEAD_TIME_MAX = 100;

    /** URL 전반(website/logoUrl/coverImageUrl/videoUrl/이미지 URL). 컬럼이 varchar(255) 다. */
    public static final int URL_MAX = 255;

    /** 본문 성격의 TEXT 컬럼. DB 상한은 없고 "얼마까지 쓰게 할 것인가" 의 제품 결정이다. */
    public static final int CONTENT_MAX = 5000;
    public static final int AS_INFO_MAX = 1000;
    public static final int ACHIEVEMENTS_MAX = 2000;

    public static final int CONTACT_NAME_MAX = 50;
    public static final int CONTACT_POSITION_MAX = 50;
    public static final int PHONE_MAX = 20;
    public static final int EMAIL_MAX = 255;

    public static final int PROJECT_TITLE_MAX = 200;
    public static final int PARTNERS_MAX = 200;
    public static final int PERIOD_MAX = 50;

    public static final int TAG_NAME_MAX = 20;
    public static final int MATERIAL_NAME_MAX = 50;
    public static final int EQUIPMENT_NAME_MAX = 50;

    public static final int CUSTOM_CERTIFICATION_NAME_MAX = 100;

    // ===== 개수 =====

    public static final int TAGS_MAX = 10;
    public static final int MATERIALS_MAX = 20;
    public static final int EQUIPMENTS_MAX = 20;
    public static final int CERTIFICATIONS_MAX = 10;
    public static final int COUNTRIES_MAX = 20;
    public static final int DOMESTIC_REGIONS_MAX = 20;
    public static final int INDUSTRIES_MAX = 5;

    /** 연락처·레퍼런스는 초기 버전상 각각 대표 1건만 받는다. 다건 허용 시 이 값을 푼다. */
    public static final int CONTACTS_MAX = 1;
    public static final int REFERENCES_MAX = 1;

    // ===== 등급 천장 =====
    //
    // 아래 셋은 등급에 따라 실제 한도가 달라지는 컬렉션의 '절대 천장'이다. 등급별 한도는 여전히
    // GradePolicy/GradePolicyRegistry 가 소유하고 서비스 계층 정책이 강제한다 — 여기 값은 그중
    // 최고 등급값이라, 어떤 등급이든 이 수를 넘을 수 없다는 뜻일 뿐 등급 한도를 대신하지 않는다.
    //
    // DTO 에 천장을 두는 이유: 명백한 남용(카테고리 5000개)을 서비스까지 들여보내지 않기 위해서다.
    // 부작용 하나는 감수한 것이다 — 관리자 등록 회사(ADMIN_EXEMPT)는 등급 한도에서 면제되지만
    // 이 천장은 지킨다. 면제의 의미가 "무제한" 이 아니라 "최고등급 대우" 가 된다.
    //
    // 같은 숫자가 두 곳(여기와 레지스트리)에 사는 상태라, 레지스트리의 최고값이 이 천장을 넘지 않는지
    // CompanyConstraintsCeilingTest 가 지킨다. 그 테스트가 없으면 나중에 등급 정책을 올렸을 때
    // 이 천장이 조용히 먼저 막는다.

    public static final int CATEGORIES_CEILING = 10;
    public static final int DETAIL_IMAGES_CEILING = 30;
    public static final int REFERENCE_IMAGES_CEILING = 10;
}
