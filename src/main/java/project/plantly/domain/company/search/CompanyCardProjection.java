package project.plantly.domain.company.search;

import project.plantly.domain.company.enums.CompanyVisibility;
import project.plantly.domain.company.search.dto.CompanySummary;

import java.util.List;

/**
 * 카드 SQL 이 읽어온 행 하나. <b>응답이 아니라 행이다</b> — 그래서 {@code search/dto} 가 아니라
 * SQL 을 소유한 {@link CompanyCardSql} 옆에 둔다.
 *
 * <p>이 타입을 따로 두는 이유는 <b>같은 행이 경로마다 다른 응답이 되기 때문</b>이다. 공개 검색·메인 노출·
 * 즐겨찾기는 공개 범위를 실어 보내지 않고, 소유자 목록만 실어 보낸다. 예전에는 RowMapper 가
 * {@link CompanySummary} 를 직접 만들어서 그 갈림길을 표현할 자리가 없었다 — 응답에 필드를 하나 더
 * 붙이려면 네 경로 전부에 붙거나 아무 데도 못 붙었다.
 *
 * <p>이제 갈림길은 이름으로 드러난다: {@code CompanySummary::fromPublic} / {@code ::fromOwner}.
 * 호출부의 {@code .map(...)} 한 줄만 보고 "이 목록이 무엇을 내보내는지" 를 알 수 있다.
 *
 * <p>{@code address} 는 원본 도로명이 아니라 이미 잘린 표시용 라벨이다({@link CompanyCardSql} 의 RowMapper 가
 * {@code RegionLabels} 로 파생) — 세종처럼 시군구 단계가 없는 예외를 한 곳에서만 다루기 위해서다.
 *
 * <p>{@code visibility} 는 <b>모든 경로가 읽어온다.</b> company 행의 스칼라라 조인이 늘지 않고, 컬럼 목록을
 * 경로별로 쪼개면 프로젝션이 갈라진다. 공개 경로에서 이 값을 버리는 판단은 SQL 이 아니라
 * {@code fromPublic} 이 한다 — 어차피 공개 경로에는 비공개 회사가 쿼리 시점에 걸러져 들어오지 않으므로
 * 그 값은 항상 PUBLIC 이고, 응답에 실으면 상수 필드가 된다.
 */
public record CompanyCardProjection(
        Long id,
        String companyName,
        String introTitle,
        String logoUrl,
        String coverImageUrl,
        String brandColor,
        String address,
        boolean verified,
        boolean featured,
        boolean spotlight,
        List<String> categoryNames,
        List<String> tagNames,
        List<String> industryNames,
        CompanyVisibility visibility
) {
}
