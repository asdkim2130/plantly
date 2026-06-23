package project.plantly.domain.user.policy;

import org.springframework.stereotype.Component;
import project.plantly.domain.user.enums.UserGrade;

import java.util.Map;

// 등급 → 정책 매핑의 단일 출처(Single Source of Truth).
// 등급별 모든 제약 사항을 여기서 한 번에 정의/관리한다.
// 등급을 추가했는데 정책 정의를 빠뜨리면 of() 에서 즉시 실패하므로 누락을 조기에 잡는다.
@Component
public class GradePolicyRegistry {

    // 필드 순서: maxCompanyCategories, videoAllowed, maxReferenceImages, maxDetailImages, customBrandColorAllowed, spotlightOnCreate
    private final Map<UserGrade, GradePolicy> policies = Map.of(
            UserGrade.FREE,             new GradePolicy(1, false, 0, 3,  false, false),
            UserGrade.BASIC,            new GradePolicy(2,  false, 0,  5,  false, false),
            UserGrade.STANDARD,         new GradePolicy(5,  true,  0,  10, true,  false),
            UserGrade.PREMIUM,          new GradePolicy(10, true,  0,  20, true,  true),
            UserGrade.ENTERPRISE,       new GradePolicy(10, true,  10, 30, true,  true),
            UserGrade.ENTERPRISE_TRIAL, new GradePolicy(10, true,  10, 30, true,  true)
    );

    public GradePolicy of(UserGrade grade) {
        GradePolicy policy = policies.get(grade);
        if (policy == null) {
            throw new IllegalStateException("등급 정책이 정의되지 않았습니다: " + grade);
        }
        return policy;
    }
}