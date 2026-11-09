-- Apply once to an existing H2 database after taking a backup. Existing owners and temperatures stay unchanged.
UPDATE student SET temp=990 WHERE temp>990;
CREATE INDEX ix_sales_seller_member ON sales(seller_member_id);
UPDATE sales SET seller_member_id=(SELECT st.member_id FROM student st WHERE st.unisno=sales.seller) WHERE seller_member_id IS NULL;
CREATE OR REPLACE VIEW sale_listing AS
SELECT s.selno,s.seller,s.seller_member_id,s.category,s.name,s.oprice,s.buyprice,s.wishprice,s.detail,s.discount,s.starttime,s.endtime,s.regtime,s.status,s.photo1,s.photo2,s.photo3,s.location,s.icon,s.bumped_at,
    COALESCE(CAST(st.temp AS DECIMAL(5,1))/10,s.seller_temperature) AS seller_temperature,
    CAST(CASE WHEN status = '판매중' AND discount > 0 AND starttime <= CURRENT_TIMESTAMP AND endtime > CURRENT_TIMESTAMP
        THEN FLOOR(CAST(oprice AS DECIMAL(18,2)) * (100 - discount) / 100) ELSE oprice END AS INT) AS current_price,
    CASE WHEN status = '거래완료' THEN 4 WHEN status = '예약중' THEN 3
         WHEN regtime > DATEADD('HOUR', -24, CURRENT_TIMESTAMP) THEN 0
         WHEN bumped_at IS NOT NULL AND bumped_at > DATEADD('HOUR', -24, CURRENT_TIMESTAMP) THEN 1
         ELSE 2 END AS exposure_priority
FROM sales s LEFT JOIN student st ON st.unisno=s.seller AND (s.seller_member_id IS NULL OR st.member_id=s.seller_member_id);

