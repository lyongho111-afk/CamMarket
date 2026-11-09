-- 기본 노출 순서: 최신 > 끌어올림 > 일반 판매중(온도순) > 예약중 > 거래완료
SELECT selno,name,current_price,status,exposure_priority FROM sale_listing
ORDER BY exposure_priority,
CASE WHEN exposure_priority=0 THEN regtime END DESC,
CASE WHEN exposure_priority=1 THEN bumped_at END DESC,
CASE WHEN exposure_priority=2 THEN seller_temperature END DESC,
regtime DESC,selno DESC;

-- Java ProductMapper의 기존 조회 컬럼
SELECT id,title,price,category,location,icon FROM products ORDER BY id DESC;

-- 예약 가격: 할인 종료와 무관하게 agreed_price를 사용
SELECT o.order_id,s.name,o.agreed_price,o.status FROM trade_orders o JOIN sales s ON s.selno=o.selno;

-- H2 예시: 20% / 24시간 할인 시작 (숫자 8과 판매자 ID는 실제 값으로 변경)
UPDATE sales SET discount=20,starttime=CURRENT_TIMESTAMP,endtime=DATEADD('HOUR',24,CURRENT_TIMESTAMP)
WHERE selno=8 AND seller='demo-seller' AND status='판매중' AND (discount=0 OR endtime<=CURRENT_TIMESTAMP);

-- 할인 종료
UPDATE sales SET discount=0,starttime=NULL,endtime=NULL WHERE selno=8 AND seller='demo-seller';

-- 끌어올리기: 사용 간격을 SQL 조건에서도 검사
UPDATE sales SET bumped_at=CURRENT_TIMESTAMP WHERE selno=4 AND seller='demo-seller' AND status='판매중'
AND (bumped_at IS NULL OR bumped_at<=DATEADD('HOUR',-24,CURRENT_TIMESTAMP));
