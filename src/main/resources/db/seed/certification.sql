-- =====================================================================
--  플랜틀리 인증 마스터 baseline 데이터 (8건)
--  성격: 관리자 소유 가변 데이터. 시드는 부트스트랩용이며, 개정·추가·폐기는
--        관리자 API 가 소유(폐기는 active=false 소프트 삭제 → 재기동 시 부활 방지).
--  slug = URL 슬러그. icon_url / description 은 미시드(생략 → NULL).
--  type = 경영시스템/산업특화/시장진입 그룹핑. 엔티티에 type 필드 필요.
--         (@Enumerated(STRING) enum 이면 상수명이 아래 값과 일치해야 함)
--  실행: ApplicationRunner > ResourceDatabasePopulator (category/industry 동일).
--  멱등: ON CONFLICT DO NOTHING. slug 의 UNIQUE 제약에 의존.
-- =====================================================================

INSERT INTO certification (certification_name, slug, type, display_order, active, created_at, updated_at)
VALUES
    ('ISO 9001 (품질경영)', 'iso-9001', 'MANAGEMENT_SYSTEM', 1, TRUE, now(), now()),
    ('ISO 14001 (환경경영)', 'iso-14001', 'MANAGEMENT_SYSTEM', 2, TRUE, now(), now()),
    ('ISO 45001 (안전보건경영)', 'iso-45001', 'MANAGEMENT_SYSTEM', 3, TRUE, now(), now()),
    ('IATF 16949 (자동차 품질경영)', 'iatf-16949', 'INDUSTRY_SPECIFIC', 4, TRUE, now(), now()),
    ('SQ 인증 (자동차 OEM 협력사 품질)', 'sq', 'INDUSTRY_SPECIFIC', 5, TRUE, now(), now()),
    ('KC 인증', 'kc', 'MARKET_ACCESS', 6, TRUE, now(), now()),
    ('CE 마킹', 'ce', 'MARKET_ACCESS', 7, TRUE, now(), now()),
    ('UL 인증', 'ul', 'MARKET_ACCESS', 8, TRUE, now(), now())
ON CONFLICT DO NOTHING;
