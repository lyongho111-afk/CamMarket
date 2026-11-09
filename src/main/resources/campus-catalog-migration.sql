-- H2: apply to an existing database; school entity columns are updated by JPA update or the instructions in docs.


CREATE OR REPLACE VIEW sale_listing AS
SELECT s.school_id,s.selno,s.seller,s.seller_member_id,s.category,s.name,s.oprice,s.buyprice,s.wishprice,s.detail,s.discount,s.starttime,s.endtime,s.regtime,s.status,s.photo1,s.photo2,s.photo3,s.location,s.icon,s.bumped_at,
    COALESCE(CAST(st.temp AS DECIMAL(5,1))/10,s.seller_temperature) AS seller_temperature,
    CAST(CASE WHEN status = '판매중' AND discount > 0 AND starttime <= CURRENT_TIMESTAMP AND endtime > CURRENT_TIMESTAMP
        THEN FLOOR(CAST(oprice AS DECIMAL(18,2)) * (100 - discount) / 100) ELSE oprice END AS INT) AS current_price,
    CASE WHEN status = '거래완료' THEN 4 WHEN status = '예약중' THEN 3
         WHEN regtime > DATEADD('HOUR', -24, CURRENT_TIMESTAMP) THEN 0
         WHEN bumped_at IS NOT NULL AND bumped_at > DATEADD('HOUR', -24, CURRENT_TIMESTAMP) THEN 1
         ELSE 2 END AS exposure_priority
FROM sales s LEFT JOIN student st ON st.unisno=s.seller AND (s.seller_member_id IS NULL OR st.member_id=s.seller_member_id);
CREATE OR REPLACE VIEW engagement_targets AS
 SELECT 'SALE' AS target_type,selno AS target_id,name AS title,location AS subtitle,current_price AS price,photo1 AS photo,selno AS parent_id,school_id FROM sale_listing
 UNION ALL SELECT 'POST',post_id,title,SUBSTRING(content,1,160),NULL,NULL,post_id,school_id FROM board_posts
 UNION ALL SELECT 'SHOP',shopId,shopname,address,NULL,photo,shopId,school_id FROM shops WHERE checked=TRUE
 UNION ALL SELECT 'COMMENT',comment_id,SUBSTRING(c.content,1,100),p.title,NULL,NULL,c.post_id,p.school_id FROM board_comments c JOIN board_posts p ON p.post_id=c.post_id;
