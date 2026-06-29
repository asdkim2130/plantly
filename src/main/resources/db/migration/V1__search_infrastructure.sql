-- 검색 인프라 (Flyway 소유 독립 섬). 엔티티 테이블은 ddl-auto 가 관리하므로 여기서 건드리지 않는다.
-- pg_trgm: ILIKE substring 매칭을 GIN 인덱스로 가속하기 위한 확장.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 회사당 1행의 비정규화 검색 도큐먼트. FK 없음(독립 섬) — 무결성은 동기화 writer 가 보장한다.
-- 1:N 자식(레퍼런스/설비/소재)은 string_agg 로 회사당 1컬럼에 평탄화한다.
CREATE TABLE company_search_document (
    company_id      BIGINT PRIMARY KEY,
    -- (A) Company 스칼라 자유텍스트
    company_name    TEXT,
    intro_title     TEXT,
    content         TEXT,
    ceo_name        TEXT,
    address         TEXT,
    detail_address  TEXT,
    lead_time       TEXT,
    as_info         TEXT,
    -- (B) 1:N 자식 집계 (string_agg)
    reference_text  TEXT,   -- projectTitle + achievements + partners
    equipment_text  TEXT,   -- equipmentName
    material_text   TEXT,   -- materialName
    -- 통합 키워드 검색용 전체 concat
    search_all      TEXT,
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- 통합검색 hot path: search_all ILIKE '%q%' 가속. 컬럼별 GIN(고급검색)은 P3 에서 V2 로 비파괴 추가.
CREATE INDEX idx_csd_search_all_trgm
    ON company_search_document USING gin (search_all gin_trgm_ops);

-- 카테고리 패싯의 서브트리 매칭용 조상 closure. 회사-카테고리 연결의 조상체인까지 펼친 id 집합.
-- 대분류 선택 시: 선택 id 가 closure 에 있으면 매칭 → hot path 에 트리연산 없이 membership 으로 처리.
CREATE TABLE company_category_closure (
    company_id   BIGINT NOT NULL,
    category_id  BIGINT NOT NULL,
    PRIMARY KEY (company_id, category_id)
);

-- 패싯 필터: category_id IN (...) -> company_id 역방향 조회 가속.
CREATE INDEX idx_ccc_category ON company_category_closure (category_id);
