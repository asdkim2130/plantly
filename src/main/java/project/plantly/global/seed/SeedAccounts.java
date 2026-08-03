package project.plantly.global.seed;

import java.util.List;

/**
 * 시드가 만든 계정 묶음. 뷰어 축을 이름으로 고정한다 — 권한 검증은 "회사를 몇 개 만들었나"가 아니라
 * "누구의 눈으로 보는가"로 갈리므로, 이 다섯 + 익명이 실질적인 테스트 매트릭스의 한 축이다.
 *
 * @param owner1    회사 다수 소유 + 즐겨찾기 다수 + 초안 보유. 프론트 주 작업 계정
 * @param owner2    회사 1건 소유. owner1 의 권한 격리 상대(남의 회사 수정 시도)
 * @param empty     회사 0 / 즐겨찾기 0. 빈 상태 화면
 * @param suspended 정지 계정. 로그인 차단
 * @param admin     관리자. 관리자 전용 뷰·플래그·복구
 */
public record SeedAccounts(
        SeedAccount owner1,
        SeedAccount owner2,
        SeedAccount empty,
        SeedAccount suspended,
        SeedAccount admin
) {

    public List<SeedAccount> all() {
        return List.of(owner1, owner2, empty, suspended, admin);
    }
}
