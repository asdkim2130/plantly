package project.plantly.domain.company.search;

/**
 * 관리자 회사 목록 질의 계약. 모든 조건 선택적 — 지정된 것만 AND(교집합)로 적용된다(미지정=전체).
 *
 * <p>불리언 3개와 deleted 는 {@code Boolean}(nullable)로 3-상태를 표현한다:
 * {@code null}=상관없음(필터 안 함), {@code true}/{@code false}=해당 값만. 그래서 verified=true·featured=true·
 * spotlight=null 이면 verified·featured 교집합을 가져오고 spotlight 는 무시된다. companyName 은 부분일치,
 * ownerUserId 는 정확 등치다.
 */
public record AdminCompanySearchCriteria(
        Boolean verified,
        Boolean featured,
        Boolean spotlight,
        // 기본(null)=삭제 포함 전체, true=삭제만, false=활성만
        Boolean deleted,
        // 회사명 부분일치(없으면 조건 없음)
        String companyName,
        // 소유자 유저 id 정확 등치(없으면 조건 없음)
        Long ownerUserId
) {}
