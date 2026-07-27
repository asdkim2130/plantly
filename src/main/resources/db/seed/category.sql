-- =====================================================================
--  플랜틀리 카테고리 마스터 데이터 (대분류 4 / 중분류 21 / 소분류 118)
--  출처: 01_플랜틀리_MVP홈페이지_기능정의서_세부.xlsx > Sheet1!B4:D25
--  slug = URL 슬러그. 부모 연결은 slug 셀프 조인으로 해석(ID 하드코딩 없음).
--  실행: ApplicationRunner > ResourceDatabasePopulator (country/region 과 동일).
--        ddl-auto 가 category 테이블 생성 후, 컨텍스트 기동 완료 시점에 로드됨.
--  멱등: ON CONFLICT DO NOTHING. slug 의 UNIQUE 제약(@Column(unique=true))에 의존함.
--        UNIQUE 제약이 없으면 재기동 시 중복 적재되므로, dev ddl-auto 가
--        create/update 로 UNIQUE 인덱스를 실제 생성하는지 확인할 것.
-- =====================================================================

-- ---------- depth 1 : 대분류 (4건) ----------
INSERT INTO category (parent_id, category_name, slug, depth, display_order, active, created_at, updated_at)
SELECT NULL, v.name, v.slug, 1, v.ord, TRUE, now(), now()
FROM (VALUES
    ('manufacturing-solution', '제조 솔루션', 1),
    ('manufacturing-infra', '제조 인프라', 2),
    ('environment-esg', '환경·ESG', 3),
    ('factory-operation', '공장 운영', 4)
) AS v(slug, name, ord)
ON CONFLICT DO NOTHING;

-- ---------- depth 2 : 중분류 (21건) ----------
INSERT INTO category (parent_id, category_name, slug, depth, display_order, active, created_at, updated_at)
SELECT p.id, v.name, v.slug, 2, v.ord, TRUE, now(), now()
FROM (VALUES
    ('die-mold', 'manufacturing-solution', '금형(Die & Mold)', 1),
    ('automation-robot', 'manufacturing-solution', '자동화/로봇', 2),
    ('assembly-fastening', 'manufacturing-solution', '조립/체결 설비', 3),
    ('machining-forming', 'manufacturing-solution', '가공/성형 설비', 4),
    ('inspection-measurement', 'manufacturing-solution', '검사/측정 설비', 5),
    ('conveying-logistics', 'manufacturing-solution', '이송/물류 설비', 6),
    ('jig-container', 'manufacturing-solution', '지그/용기', 7),
    ('production-process-management', 'manufacturing-infra', '생산/공정 관리', 1),
    ('asset-logistics-management', 'manufacturing-infra', '자산/물류 관리', 2),
    ('quality-equipment-management', 'manufacturing-infra', '품질/설비 관리', 3),
    ('data-infrastructure', 'manufacturing-infra', '데이터 인프라', 4),
    ('business-etc', 'manufacturing-infra', '비즈니스/기타', 5),
    ('industrial-cleaning', 'environment-esg', '산업용 전문 청소', 1),
    ('environmental-air-treatment', 'environment-esg', '환경 정화/대기', 2),
    ('safety-disaster-prevention', 'environment-esg', '안전/방재', 3),
    ('regulation-certification', 'environment-esg', '규제/인증', 4),
    ('machining-assembly-consumables', 'factory-operation', '가공/조립 소모품', 1),
    ('hydraulic-piping-materials', 'factory-operation', '유압/배관 자재', 2),
    ('electrical-control-materials', 'factory-operation', '전기/제어 자재', 3),
    ('safety-protective-equipment', 'factory-operation', '현장 안전 보호구', 4),
    ('facility-maintenance', 'factory-operation', '시설 유지보수', 5)
) AS v(slug, parent_slug, name, ord)
JOIN category p ON p.slug = v.parent_slug
ON CONFLICT DO NOTHING;

-- ---------- depth 3 : 소분류 (118건) ----------
INSERT INTO category (parent_id, category_name, slug, depth, display_order, active, created_at, updated_at)
SELECT p.id, v.name, v.slug, 3, v.ord, TRUE, now(), now()
FROM (VALUES
    ('hot-runner-injection-mold', 'die-mold', '핫런너 사출금형', 1),
    ('two-shot-injection-mold', 'die-mold', '이색/이중 사출금형', 2),
    ('insert-mold', 'die-mold', '인서트 금형', 3),
    ('progressive-press-die', 'die-mold', '프로그레시브 프레스금형', 4),
    ('tandem-die', 'die-mold', '탠덤 금형', 5),
    ('die-casting-mold', 'die-mold', '다이캐스팅 금형', 6),
    ('cartesian-robot', 'automation-robot', '직교로봇', 1),
    ('articulated-robot-6axis', 'automation-robot', '다관절로봇(6축)', 2),
    ('collaborative-robot', 'automation-robot', '협동로봇(Cobot)', 3),
    ('scara-robot', 'automation-robot', '스카라로봇', 4),
    ('robot-gripper', 'automation-robot', '로봇 그리퍼 제작', 5),
    ('robot-si-customizing', 'automation-robot', '로봇 SI 커스터마이징', 6),
    ('auto-screw-fastening', 'assembly-fastening', '자동 나사체결기(Servo)', 1),
    ('riveting-machine', 'assembly-fastening', '리벳팅기', 2),
    ('ultrasonic-welder', 'assembly-fastening', '초음파 융착기', 3),
    ('heat-welder', 'assembly-fastening', '열융착기', 4),
    ('press-fitting-machine', 'assembly-fastening', '압입기(Press)', 5),
    ('dispenser', 'assembly-fastening', '정량 토출기(Dispenser)', 6),
    ('cnc-lathe-milling', 'machining-forming', 'CNC 선반/밀링', 1),
    ('five-axis-machining-center', 'machining-forming', '5축 가공기', 2),
    ('laser-cutting-welding', 'machining-forming', '레이저 커팅/용접기', 3),
    ('plasma-cutter', 'machining-forming', '플라즈마 절단기', 4),
    ('injection-molding-machine', 'machining-forming', '사출 성형기', 5),
    ('foaming-machine', 'machining-forming', '발포기', 6),
    ('rubber-processing-machine', 'machining-forming', '고무 가공기', 7),
    ('vision-inspection-module', 'inspection-measurement', '비전 검사 모듈(2D/3D)', 1),
    ('ai-deep-learning-inspection', 'inspection-measurement', 'AI 딥러닝 검사', 2),
    ('eol-test', 'inspection-measurement', 'EOL(최종 성능 검사)', 3),
    ('leak-test', 'inspection-measurement', '누설 검사(Leak)', 4),
    ('dimensional-measurement-automation', 'inspection-measurement', '치수 측정 자동화', 5),
    ('harness-inspection', 'inspection-measurement', '하네스 검사', 6),
    ('agv', 'conveying-logistics', 'AGV(무인반송차)', 1),
    ('amr', 'conveying-logistics', 'AMR(자율주행로봇)', 2),
    ('overhead-conveyor', 'conveying-logistics', '오버헤드 컨베이어', 3),
    ('chain-conveyor', 'conveying-logistics', '체인 컨베이어', 4),
    ('sorter', 'conveying-logistics', '소터(분류기)', 5),
    ('vertical-lifter', 'conveying-logistics', '수직 반송기', 6),
    ('assembly-jig', 'jig-container', '조립 지그', 1),
    ('welding-fixture', 'jig-container', '용접 치구', 2),
    ('machining-fixture', 'jig-container', '가공 치구', 3),
    ('checking-fixture', 'jig-container', '검사구(Checking Fixture)', 4),
    ('steel-pallet', 'jig-container', '철제 파렛트', 5),
    ('corrugated-plastic-box', 'jig-container', '단프라 박스', 6),
    ('loading-cart', 'jig-container', '적재용 전용 대차', 7),
    ('cloud-mes', 'production-process-management', '클라우드 MES', 1),
    ('onpremise-mes', 'production-process-management', '온프레미스 MES', 2),
    ('aps', 'production-process-management', 'APS(생산계획)', 3),
    ('cost-management-system', 'production-process-management', '원가 관리 시스템', 4),
    ('process-tracking', 'production-process-management', '공정 트래킹', 5),
    ('equipment-monitoring', 'production-process-management', '설비 모니터링', 6),
    ('wms', 'asset-logistics-management', 'WMS(창고관리)', 1),
    ('tms', 'asset-logistics-management', 'TMS(배송관리)', 2),
    ('barcode-rfid', 'asset-logistics-management', '바코드/RFID 시스템', 3),
    ('inventory-optimization', 'asset-logistics-management', '재고 최적화 솔루션', 4),
    ('material-supply-management', 'asset-logistics-management', '자재 수급 관리', 5),
    ('qms', 'quality-equipment-management', 'QMS(품질관리)', 1),
    ('lims', 'quality-equipment-management', 'LIMS(실험실정보)', 2),
    ('spc', 'quality-equipment-management', 'SPC(통계적공정제어)', 3),
    ('predictive-maintenance', 'quality-equipment-management', '설비 예지보전(PdM)', 4),
    ('equipment-utilization-analysis', 'quality-equipment-management', '설비 가동률 분석', 5),
    ('scada', 'data-infrastructure', 'SCADA', 1),
    ('plc-data-collection', 'data-infrastructure', 'PLC 데이터 수집', 2),
    ('opc-ua-integration', 'data-infrastructure', 'OPC-UA 연동', 3),
    ('manufacturing-bigdata-analytics', 'data-infrastructure', '제조 빅데이터 분석', 4),
    ('edge-computing', 'data-infrastructure', '엣지 컴퓨팅', 5),
    ('factory-digital-twin', 'data-infrastructure', '공장 디지털트윈', 6),
    ('erp-integration', 'business-etc', 'ERP 연동 솔루션', 1),
    ('plm', 'business-etc', 'PLM(제품수명주기)', 2),
    ('groupware', 'business-etc', '그룹웨어', 3),
    ('manufacturing-collaboration-platform', 'business-etc', '제조 협업 플랫폼', 4),
    ('drawing-management-system', 'business-etc', '도면 관리 시스템', 5),
    ('cooling-tower-sludge-removal', 'industrial-cleaning', '냉각탑 슬러지 제거', 1),
    ('coolant-tank-cleaning', 'industrial-cleaning', '수용성/유성 탱크 청소', 2),
    ('chip-conveyor-cleaning', 'industrial-cleaning', '칩 컨베이어 청소', 3),
    ('duct-cleaning', 'industrial-cleaning', '배관 덕트 크리닝', 4),
    ('oil-skimmer-installation', 'industrial-cleaning', '오일 스키머 설치', 5),
    ('dust-collector', 'environmental-air-treatment', '집진기(필터/원심)', 1),
    ('scrubber', 'environmental-air-treatment', '스크라바(화학/수처리)', 2),
    ('voc-reduction-system', 'environmental-air-treatment', 'VOC 저감 장치', 3),
    ('air-pollution-control-design', 'environmental-air-treatment', '대기오염 방지 시설 설계', 4),
    ('water-treatment-filtration', 'environmental-air-treatment', '수처리 필터링', 5),
    ('industrial-safety-monitoring', 'safety-disaster-prevention', '산업안전 모니터링', 1),
    ('intelligent-cctv', 'safety-disaster-prevention', '지능형 CCTV(안전모 미착용 감지)', 2),
    ('fire-detection-system', 'safety-disaster-prevention', '화재 감지 시스템', 3),
    ('fall-protection-system', 'safety-disaster-prevention', '추락 방지 시설', 4),
    ('storage-facility-inspection', 'safety-disaster-prevention', '보관시설 안전점검', 5),
    ('esg-consulting', 'regulation-certification', 'ESG 경영 컨설팅', 1),
    ('carbon-emission-measurement', 'regulation-certification', '탄소 배출량 측정', 2),
    ('iso-certification-agency', 'regulation-certification', 'ISO 인증 심사 대행', 3),
    ('environmental-permit-report', 'regulation-certification', '환경 인허가 보고서', 4),
    ('industrial-safety-diagnosis', 'regulation-certification', '산업 안전 진단', 5),
    ('power-air-tools', 'machining-assembly-consumables', '전동/에어 공구', 1),
    ('cutting-insert-drill-bit', 'machining-assembly-consumables', '절삭 팁/드릴 비트', 2),
    ('grinding-wheel', 'machining-assembly-consumables', '연마석', 3),
    ('screw-bolt-nut', 'machining-assembly-consumables', '나사/볼트/너트류', 4),
    ('hand-tool-set', 'machining-assembly-consumables', '수공구 세트', 5),
    ('hydraulic-hose', 'hydraulic-piping-materials', '유압 호스', 1),
    ('valve', 'hydraulic-piping-materials', '밸브', 2),
    ('cylinder-parts', 'hydraulic-piping-materials', '실린더 부품', 3),
    ('industrial-gas', 'hydraulic-piping-materials', '산업용 가스', 4),
    ('compressor-consumables', 'hydraulic-piping-materials', '컴프레셔 소모품', 5),
    ('lubricant-grease', 'hydraulic-piping-materials', '윤활유/그리스', 6),
    ('plc-module', 'electrical-control-materials', 'PLC 모듈', 1),
    ('proximity-photo-sensor', 'electrical-control-materials', '센서류(근접/광)', 2),
    ('switch-relay', 'electrical-control-materials', '스위치/릴레이', 3),
    ('industrial-cable', 'electrical-control-materials', '산업용 케이블', 4),
    ('control-panel-parts', 'electrical-control-materials', '제어반 부품', 5),
    ('safety-shoes', 'safety-protective-equipment', '안전화', 1),
    ('workwear', 'safety-protective-equipment', '고성능 작업복', 2),
    ('chemical-resistant-gloves', 'safety-protective-equipment', '내화학 장갑', 3),
    ('dust-gas-mask', 'safety-protective-equipment', '방진/방독 마스크', 4),
    ('ear-muffs', 'safety-protective-equipment', '귀덮개', 5),
    ('safety-helmet', 'safety-protective-equipment', '안전모', 6),
    ('factory-electrical-work', 'facility-maintenance', '공장 전기 공사', 1),
    ('piping-repair', 'facility-maintenance', '배관 설비 보수', 2),
    ('forklift-rental-repair', 'facility-maintenance', '지게차 임대/수리', 3),
    ('factory-led-lighting', 'facility-maintenance', '공장 조명(LED) 교체', 4),
    ('partition-installation', 'facility-maintenance', '현장 칸막이 공사', 5)
) AS v(slug, parent_slug, name, ord)
JOIN category p ON p.slug = v.parent_slug
ON CONFLICT DO NOTHING;
