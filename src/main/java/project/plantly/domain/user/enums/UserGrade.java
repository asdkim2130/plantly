package project.plantly.domain.user.enums;

public enum UserGrade {
    // 괄호 값 = 등급별 회사 카테고리(CompanyCategory) 최대 저장 개수
    BASIC(2),
    ENTERPRISE(10),
    ENTERPRISE_TRIAL(10),
    STANDARD(5),
    PREMIUM(10);

    private final int maxCompanyCategories;

    UserGrade(int maxCompanyCategories) {
        this.maxCompanyCategories = maxCompanyCategories;
    }

    public int getMaxCompanyCategories() {
        return this.maxCompanyCategories;
    }
}
