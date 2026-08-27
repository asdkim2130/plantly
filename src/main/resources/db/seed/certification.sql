-- =====================================================================
--  플랜틀리 인증 마스터 baseline 데이터 (29건)
--  성격: 관리자 소유 가변 데이터. 시드는 부트스트랩용이며, 개정·추가·폐기는
--        관리자 API 가 소유(폐기는 active=false 소프트 삭제 → 재기동 시 부활 방지).
--  slug = URL 슬러그. icon_url / description 은 미시드(생략 → NULL).
--  type = 경영시스템/산업특화/시장진입 그룹핑. 엔티티에 type 필드 필요.
--         (@Enumerated(STRING) enum 이면 상수명이 아래 값과 일치해야 함)
--  실행: ApplicationRunner > ResourceDatabasePopulator (category/industry 동일).
--  멱등: ON CONFLICT DO NOTHING. slug 의 UNIQUE 제약에 의존.
--
--  display_order 정책: 그룹별 100 단위 블록, 그룹 내 10 단위 간격.
--    100번대 = MANAGEMENT_SYSTEM
--    200~300번대 = INDUSTRY_SPECIFIC (업종 계열별로 뭉쳐서 부여, 210~390)
--    400번대     = MARKET_ACCESS (410~450)
--    999         = 기타(자유입력 앵커 row)
--
--  '기타' row: company_certification.custom_name 자유입력의 FK 앵커.
--             UI 최하단 고정. 삭제/비활성 금지.
-- =====================================================================

INSERT INTO certification (certification_name, slug, type, display_order, active, created_at, updated_at)
VALUES
    -- ── 경영시스템 인증 (업종 무관) ────────────────────────────────────
    ('ISO 9001 (품질경영)',              'iso-9001',   'MANAGEMENT_SYSTEM', 110, TRUE, now(), now()),
    ('ISO 14001 (환경경영)',             'iso-14001',  'MANAGEMENT_SYSTEM', 120, TRUE, now(), now()),
    ('ISO 45001 (안전보건경영)',          'iso-45001',  'MANAGEMENT_SYSTEM', 130, TRUE, now(), now()),
    ('ISO 50001 (에너지경영)',            'iso-50001',  'MANAGEMENT_SYSTEM', 140, TRUE, now(), now()),

    -- ── 산업 특화 인증 : 자동차 ───────────────────────────────────────
    ('IATF 16949 (자동차 품질경영)',       'iatf-16949', 'INDUSTRY_SPECIFIC', 210, TRUE, now(), now()),
    ('SQ 인증 (자동차 OEM 협력사 품질)',    'sq',         'INDUSTRY_SPECIFIC', 220, TRUE, now(), now()),
    ('ISO 26262 (자동차 기능안전)',        'iso-26262',  'INDUSTRY_SPECIFIC', 230, TRUE, now(), now()),

    -- ── 산업 특화 인증 : 기계 · 금속가공 ──────────────────────────────
    ('ISO 3834 (용접품질)',              'iso-3834',        'INDUSTRY_SPECIFIC', 240, TRUE, now(), now()),
    ('ASME Stamp (압력용기)',            'asme-stamp',      'INDUSTRY_SPECIFIC', 250, TRUE, now(), now()),
    ('S마크 안전인증',                    's-mark',          'INDUSTRY_SPECIFIC', 260, TRUE, now(), now()),
    ('KGS 인증 (가스기기)',               'kgs',             'INDUSTRY_SPECIFIC', 270, TRUE, now(), now()),
    ('방폭인증',                          'explosion-proof', 'INDUSTRY_SPECIFIC', 280, TRUE, now(), now()),

    -- ── 산업 특화 인증 : 의료기기 ─────────────────────────────────────
    ('ISO 13485 (의료기기 품질경영)',      'iso-13485',          'INDUSTRY_SPECIFIC', 290, TRUE, now(), now()),
    ('의료기기 GMP',                      'medical-device-gmp', 'INDUSTRY_SPECIFIC', 300, TRUE, now(), now()),

    -- ── 산업 특화 인증 : 항공 · 철도 · 조선 · 방산 ────────────────────
    ('AS9100 (항공우주 품질경영)',         'as9100',              'INDUSTRY_SPECIFIC', 310, TRUE, now(), now()),
    ('ISO 22163 (철도 품질경영/IRIS)',     'iso-22163',           'INDUSTRY_SPECIFIC', 320, TRUE, now(), now()),
    ('선급 인증 (KR/DNV/ABS 등)',         'ship-classification', 'INDUSTRY_SPECIFIC', 330, TRUE, now(), now()),
    ('방산 품질경영시스템 (DQ)',           'defense-quality',     'INDUSTRY_SPECIFIC', 340, TRUE, now(), now()),

    -- ── 산업 특화 인증 : 식품 · 제약 ──────────────────────────────────
    ('HACCP (식품안전관리인증)',           'haccp',       'INDUSTRY_SPECIFIC', 350, TRUE, now(), now()),
    ('FSSC 22000 (식품안전시스템)',        'fssc-22000',  'INDUSTRY_SPECIFIC', 360, TRUE, now(), now()),
    ('KGMP (의약품 제조 및 품질관리)',      'kgmp',        'INDUSTRY_SPECIFIC', 370, TRUE, now(), now()),

    -- ── 산업 특화 인증 : 섬유 · 목재 ──────────────────────────────────
    ('OEKO-TEX Standard 100 (섬유 유해물질)', 'oeko-tex', 'INDUSTRY_SPECIFIC', 380, TRUE, now(), now()),
    ('FSC (산림경영/CoC)',                   'fsc',      'INDUSTRY_SPECIFIC', 390, TRUE, now(), now()),

    -- ── 시장 진입 인증 ────────────────────────────────────────────────
    ('KC 인증',                          'kc',    'MARKET_ACCESS', 410, TRUE, now(), now()),
    ('CE 마킹',                          'ce',    'MARKET_ACCESS', 420, TRUE, now(), now()),
    ('UL 인증',                          'ul',    'MARKET_ACCESS', 430, TRUE, now(), now()),
    ('RoHS (유해물질 제한)',              'rohs',  'MARKET_ACCESS', 440, TRUE, now(), now()),
    ('REACH / K-REACH (화학물질 등록)',    'reach', 'MARKET_ACCESS', 450, TRUE, now(), now()),

    -- ── 기타 (자유입력 앵커) ──────────────────────────────────────────
    ('기타',                             'etc',   'ETC',          999, TRUE, now(), now())
ON CONFLICT DO NOTHING;
