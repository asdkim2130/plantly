package project.plantly.domain.company.search;

/**
 * SQL {@code LIKE/ILIKE} 부분일치 패턴 헬퍼. LIKE 메타문자(%, _, \)를 이스케이프해 사용자가 입력한
 * 검색어를 '리터럴 부분일치'로만 매칭하게 한다. 쿼리에는 반드시 {@code ESCAPE '\'} 를 함께 붙인다.
 */
public final class LikePatterns {

    private LikePatterns() {
    }

    // 검색어를 substring 매칭용 %term% 로 감싸고, 내부의 LIKE 메타문자는 이스케이프한다.
    public static String contains(String term) {
        String escaped = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
