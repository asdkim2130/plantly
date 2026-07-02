package project.plantly.domain.company.policy;

import org.springframework.stereotype.Component;
import project.plantly.domain.company.enums.CompanyGrade;

import java.util.Map;

// 등급 → 정책 매핑의 단일 출처(Single Source of Truth).
// 등급별 모든 제약 사항을 여기서 한 번에 정의/관리한다.
// 등급을 추가했는데 정책 정의를 빠뜨리면 of() 에서 즉시 실패하므로 누락을 조기에 잡는다.
// (체험/면제/만료는 등급이 아니라 CompanySubscription 의 status/expiresAt 가 표현하므로 여기엔 순수 등급만 둔다.)
@Component
public class GradePolicyRegistry {

    // 필드 순서: maxCompanyCategories, videoAllowed, maxReferenceImages, maxDetailImages, customBrandColorAllowed, spotlightOnCreate
    private final Map<CompanyGrade, GradePolicy> policies = Map.of(
            CompanyGrade.FREE,       new GradePolicy(1,  false, 0,  3,  false, false),
            CompanyGrade.BASIC,      new GradePolicy(2,  false, 0,  5,  false, false),
            CompanyGrade.STANDARD,   new GradePolicy(5,  true,  0,  10, true,  false),
            CompanyGrade.PREMIUM,    new GradePolicy(10, true,  0,  20, true,  true),
            CompanyGrade.ENTERPRISE, new GradePolicy(10, true,  10, 30, true,  true)
    );

    public GradePolicy of(CompanyGrade grade) {
        GradePolicy policy = policies.get(grade);
        if (policy == null) {
            throw new IllegalStateException("등급 정책이 정의되지 않았습니다: " + grade);
        }
        return policy;
    }
}
