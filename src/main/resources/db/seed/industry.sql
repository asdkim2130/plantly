-- =====================================================================
--  플랜틀리 산업(수요·전방산업) 마스터 데이터 (24건)
--  성격: 큐레이션 운영 데이터(service-authored). KSIC 는 참조 표준이며 미저장.
--        (필요 시 ksic 매핑 컬럼을 nullable 로 후속 추가)
--  slug = URL 슬러그. icon_url / description 은 미시드(생략 → NULL).
--  실행: ApplicationRunner > ResourceDatabasePopulator (category.sql 과 동일).
--        ddl-auto 가 industry 테이블 생성 후, 컨텍스트 기동 완료 시점에 로드됨.
--  멱등: ON CONFLICT DO NOTHING. slug / industry_name 의 UNIQUE 제약
--        (@Column(unique=true))에 의존함.
-- =====================================================================

INSERT INTO industry (industry_name, slug, display_order, active, created_at, updated_at)
VALUES
    ('자동차', 'automotive', 1, TRUE, now(), now()),
    ('반도체', 'semiconductor', 2, TRUE, now(), now()),
    ('이차전지', 'secondary-battery', 3, TRUE, now(), now()),
    ('디스플레이', 'display', 4, TRUE, now(), now()),
    ('식품', 'food-beverage', 5, TRUE, now(), now()),
    ('가전', 'home-appliance', 6, TRUE, now(), now()),
    ('의료기기', 'medical-device', 7, TRUE, now(), now()),
    ('화학', 'chemical', 8, TRUE, now(), now()),
    ('철강', 'steel', 9, TRUE, now(), now()),
    ('기계', 'machinery', 10, TRUE, now(), now()),
    ('조선·해양', 'shipbuilding-marine', 11, TRUE, now(), now()),
    ('항공·우주·방산', 'aerospace-defense', 12, TRUE, now(), now()),
    ('제약·바이오', 'pharma-bio', 13, TRUE, now(), now()),
    ('화장품', 'cosmetics', 14, TRUE, now(), now()),
    ('섬유·의류', 'textile-apparel', 15, TRUE, now(), now()),
    ('전자부품·전기장비', 'electronic-electrical-components', 16, TRUE, now(), now()),
    ('금속가공·비철금속', 'metal-processing', 17, TRUE, now(), now()),
    ('플라스틱·고무', 'plastic-rubber', 18, TRUE, now(), now()),
    ('포장·인쇄', 'packaging-printing', 19, TRUE, now(), now()),
    ('에너지·신재생', 'energy-renewable', 20, TRUE, now(), now()),
    ('세라믹·유리', 'ceramics-glass', 21, TRUE, now(), now()),
    ('건축·건설자재', 'building-materials', 22, TRUE, now(), now()),
    ('가구·목재', 'furniture-wood', 23, TRUE, now(), now()),
    ('조명', 'lighting', 24, TRUE, now(), now())
ON CONFLICT DO NOTHING;
