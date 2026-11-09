-- 캠퍼스 리마켓 / 첨부 Excel 표 기준 / H2 2.x (MODE=MySQL)
-- 새 DB 초기화용입니다. 기존 운영 DB에 대한 자동 마이그레이션/삭제는 수행하지 않습니다.
-- text(50) 등은 VARCHAR(50)으로, 비밀번호 필드는 해시 저장을 위해 VARCHAR(255)로 변환했습니다.

-- 로그인/회원가입 공통 규격: 학번 문자열, 365 = 36.5도
CREATE TABLE IF NOT EXISTS student (
                                       unisno VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY,
    member_id BIGINT UNIQUE, -- 실제 회원 ID와 연결; 다른 학교의 동일 학번을 자동 합치지 않음
    temp INT NOT NULL DEFAULT 365,
    CONSTRAINT ck_student_temp CHECK(temp BETWEEN 0 AND 990)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. shops 테이블
CREATE TABLE IF NOT EXISTS shops (
                                     school_id BIGINT,
                                     version BIGINT NOT NULL DEFAULT 0,
                                     shopId INT AUTO_INCREMENT PRIMARY KEY,
                                     owner_member_id BIGINT UNIQUE,
                                     shopname VARCHAR(50) NOT NULL,
    shopphone VARCHAR(50) NULL,
    address VARCHAR(200) NULL,
    photo VARCHAR(255) NULL,
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    busino VARCHAR(50) NULL,
    ceoname VARCHAR(50) NULL,
    busi_kind VARCHAR(50) NULL,
    bmenu VARCHAR(400) NULL,
    reservtime VARCHAR(100) NULL,
    busitime VARCHAR(100) NULL,
    offday VARCHAR(20) NULL,
    specialevent VARCHAR(50) NULL,
    sizeandtype VARCHAR(200) NULL,
    status VARCHAR(20) NOT NULL DEFAULT '영업종료',
    availablespace VARCHAR(50) NULL,
    checked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_shop_coordinates CHECK ((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)),
    CONSTRAINT ck_shop_status CHECK (status IN ('영업중','혼잡','영업종료','휴무'))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. sales 테이블
CREATE TABLE IF NOT EXISTS sales (
                                     school_id BIGINT,
                                     selno BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     seller VARCHAR(50) NOT NULL,
    seller_member_id BIGINT NULL,
    category VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    oprice INT NOT NULL, -- 표시가 / 할인 전 가격
    buyprice INT NULL, -- 판매자가 구입했던 가격 (선택)
    wishprice INT NULL, -- 판매자의 희망 매가 (선택, 할인 계산 기준과 별도)
    detail VARCHAR(1500) NULL,
    discount INT NOT NULL DEFAULT 0, -- 할인율 %, 0이면 할인 없음
    starttime TIMESTAMP NULL,
    endtime TIMESTAMP NULL,
    regtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT '판매중',
    photo1 VARCHAR(500) NULL,
    photo2 VARCHAR(500) NULL,
    photo3 VARCHAR(500) NULL,
    location VARCHAR(80) NOT NULL DEFAULT '인하대학교',
    icon VARCHAR(20) NOT NULL DEFAULT 'book',
    seller_temperature DECIMAL(5,2) NOT NULL DEFAULT 36.50,
    bumped_at TIMESTAMP NULL,
    CONSTRAINT fk_sale_seller_member FOREIGN KEY(seller_member_id) REFERENCES student(member_id),
    CONSTRAINT ck_sale_price CHECK (oprice >= 0 AND (buyprice IS NULL OR buyprice >= 0) AND (wishprice IS NULL OR wishprice >= 0)),
    CONSTRAINT ck_sale_discount CHECK (discount BETWEEN 0 AND 99),
    CONSTRAINT ck_sale_status CHECK (status IN ('판매중','예약중','거래완료')),
    CONSTRAINT ck_sale_discount_time CHECK (
(discount = 0 AND starttime IS NULL AND endtime IS NULL)
    OR (discount BETWEEN 1 AND 99 AND starttime IS NOT NULL AND endtime IS NOT NULL
        AND endtime = TIMESTAMPADD(HOUR, 24, starttime))
    ),
    CONSTRAINT ck_sale_discount_amount CHECK (discount = 0 OR FLOOR(CAST(oprice AS DECIMAL(18,2)) * (100 - discount) / 100) >= 1)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--CREATE INDEX ix_sales_status_regtime ON sales(status, regtime);
--CREATE INDEX ix_sales_status_bumped ON sales(status, bumped_at);
--CREATE INDEX ix_sales_seller ON sales(seller);
--CREATE INDEX ix_sales_seller_member ON sales(seller_member_id);

-- 4. board_categories 테이블
CREATE TABLE IF NOT EXISTS board_categories (
                                                category_id INT AUTO_INCREMENT PRIMARY KEY,
                                                category_name VARCHAR(30) NOT NULL UNIQUE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. board_posts 테이블
CREATE TABLE IF NOT EXISTS board_posts (
                                           post_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           category_id INT NOT NULL,
                                           writer VARCHAR(50) NOT NULL,
    author_member_id BIGINT NULL,
    author_name VARCHAR(20) NULL,
    school_name VARCHAR(100) NULL,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    regtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_post_category FOREIGN KEY(category_id) REFERENCES board_categories(category_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--CREATE INDEX ix_posts_category_time ON board_posts(category_id, regtime);

-- 6. board_comments 테이블
CREATE TABLE IF NOT EXISTS board_comments (
                                              comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              post_id BIGINT NOT NULL,
                                              member_id BIGINT NOT NULL,
                                              author_name VARCHAR(20) NOT NULL,
    school_name VARCHAR(100) NULL,
    content VARCHAR(1000) NOT NULL,
    regtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_post FOREIGN KEY(post_id) REFERENCES board_posts(post_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--CREATE INDEX ix_comments_post_time ON board_comments(post_id, comment_id);

-- 7. trade_orders 테이블
CREATE TABLE IF NOT EXISTS trade_orders (
                                            order_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY,
    selno BIGINT NOT NULL,
    buyer VARCHAR(50) NOT NULL,
    agreed_price INT NOT NULL CHECK(agreed_price >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'RESERVED',
    meeting_place VARCHAR(150) NULL,
    meeting_time TIMESTAMP NULL,
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    payment_key VARCHAR(300) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_sale FOREIGN KEY(selno) REFERENCES sales(selno),
    CONSTRAINT ck_order_status CHECK(status IN ('RESERVED','PAYMENT_READY','WAITING_FOR_DEPOSIT','PAID','COMPLETED','거래완료','CANCELED')),
    CONSTRAINT ck_order_coordinates CHECK ((latitude IS NULL AND longitude IS NULL) OR
(latitude IS NOT NULL AND longitude IS NOT NULL AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--CREATE INDEX ix_orders_sale ON trade_orders(selno);

-- 8. manner_ratings 테이블
CREATE TABLE IF NOT EXISTS manner_ratings (
                                              rating_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              order_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    reviewer_unisno VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    recipient_unisno VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    feedback VARCHAR(10) NOT NULL,
    delta INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rating_order FOREIGN KEY(order_id) REFERENCES trade_orders(order_id),
    CONSTRAINT fk_rating_reviewer FOREIGN KEY(reviewer_unisno) REFERENCES student(unisno),
    CONSTRAINT fk_rating_recipient FOREIGN KEY(recipient_unisno) REFERENCES student(unisno),
    CONSTRAINT uq_rating_order_reviewer UNIQUE(order_id, reviewer_unisno),
    CONSTRAINT ck_rating_feedback CHECK(feedback IN ('GOOD','BAD')),
    CONSTRAINT ck_rating_self CHECK(reviewer_unisno <> recipient_unisno)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. sale_listing 뷰 (MySQL 호환 TIMESTAMPADD 함수 사용)
CREATE OR REPLACE VIEW sale_listing AS
SELECT s.selno, s.seller, s.seller_member_id, s.category, s.name, s.oprice, s.buyprice, s.wishprice, s.detail, s.discount, s.starttime, s.endtime, s.regtime, s.status, s.photo1, s.photo2, s.photo3, s.location, s.icon, s.bumped_at,
       COALESCE(CAST(st.temp AS DECIMAL(5,1))/10, s.seller_temperature) AS seller_temperature,
       CAST(CASE WHEN status = '판매중' AND discount > 0 AND starttime <= CURRENT_TIMESTAMP AND endtime > CURRENT_TIMESTAMP
                     THEN FLOOR(CAST(oprice AS DECIMAL(18,2)) * (100 - discount) / 100) ELSE oprice END AS SIGNED) AS current_price,
       CASE WHEN status = '거래완료' THEN 4 WHEN status = '예약중' THEN 3
            WHEN regtime > TIMESTAMPADD(HOUR, -24, CURRENT_TIMESTAMP) THEN 0
            WHEN bumped_at IS NOT NULL AND bumped_at > TIMESTAMPADD(HOUR, -24, CURRENT_TIMESTAMP) THEN 1
            ELSE 2 END AS exposure_priority
FROM sales s LEFT JOIN student st ON st.unisno=s.seller AND (s.seller_member_id IS NULL OR st.member_id=s.seller_member_id);

-- 10. products 뷰
CREATE OR REPLACE VIEW products AS
SELECT selno AS id, name AS title, current_price AS price, category, location, icon FROM sale_listing;

-- 11. shop_change_logs 테이블
CREATE TABLE IF NOT EXISTS shop_change_logs (
                                                change_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                shop_id INT NOT NULL,
                                                actor_member_id BIGINT NOT NULL,
                                                actor_name VARCHAR(20) NOT NULL,
    actor_role VARCHAR(20) NOT NULL,
    field_key VARCHAR(40) NOT NULL,
    field_label VARCHAR(50) NOT NULL,
    before_value VARCHAR(500) NOT NULL,
    after_value VARCHAR(500) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shop_change_shop FOREIGN KEY(shop_id) REFERENCES shops(shopId)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--CREATE INDEX ix_shop_change_time ON shop_change_logs(shop_id, changed_at, change_id);

-- 12. admin_action_logs 테이블
CREATE TABLE IF NOT EXISTS admin_action_logs (
                                                 action_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 shop_id INT NOT NULL,
                                                 shop_name VARCHAR(50) NOT NULL,
    actor_member_id BIGINT NOT NULL,
    actor_name VARCHAR(20) NOT NULL,
    action VARCHAR(20) NOT NULL,
    before_value VARCHAR(100) NOT NULL,
    after_value VARCHAR(100) NOT NULL,
    note VARCHAR(300) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_action_shop FOREIGN KEY(shop_id) REFERENCES shops(shopId)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--CREATE INDEX ix_admin_action_time ON admin_action_logs(created_at, action_id);

-- 13. seller_notifications 테이블
CREATE TABLE IF NOT EXISTS seller_notifications (
                                                    notification_id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
    seller_member_id BIGINT NOT NULL,
    sale_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(1500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    CONSTRAINT fk_notification_order FOREIGN KEY(order_id) REFERENCES trade_orders(order_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--CREATE INDEX ix_notification_seller ON seller_notifications(seller_member_id, created_at);

-- 14. notification_deliveries 테이블
CREATE TABLE IF NOT EXISTS notification_deliveries (
                                                       delivery_id VARCHAR(36) PRIMARY KEY,
    notification_id VARCHAR(36) NOT NULL,
    channel VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempted_at TIMESTAMP NULL,
    last_error VARCHAR(300) NULL,
    CONSTRAINT uq_notification_channel UNIQUE(notification_id, channel),
    CONSTRAINT ck_notification_channel CHECK(channel IN ('SMS','EMAIL','KAKAO')),
    CONSTRAINT fk_delivery_notification FOREIGN KEY(notification_id) REFERENCES seller_notifications(notification_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--CREATE INDEX ix_notification_pending ON notification_deliveries(status);

-- 15. notification_settings 테이블
CREATE TABLE IF NOT EXISTS notification_settings (
                                                     member_id BIGINT PRIMARY KEY,
                                                     email VARCHAR(100) NULL,
    phone VARCHAR(20) NULL,
    sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    kakao_enabled BOOLEAN NOT NULL DEFAULT TRUE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. trade_reviews 테이블
CREATE TABLE IF NOT EXISTS trade_reviews (
                                             review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             order_id VARCHAR(50) NOT NULL,
    reviewer_id VARCHAR(50) NOT NULL,
    target_id VARCHAR(50) NOT NULL,
    score INT NOT NULL,
    attitude VARCHAR(50) NULL,
    content VARCHAR(500) NULL,
    temperature_delta INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_trade_review UNIQUE (order_id, reviewer_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. content_reactions 테이블
CREATE TABLE IF NOT EXISTS content_reactions (
                                                 member_id BIGINT NOT NULL,
                                                 target_type VARCHAR(10) NOT NULL,
    target_id BIGINT NOT NULL,
    kind VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(member_id, target_type, target_id, kind),
    CONSTRAINT ck_reaction_type CHECK(target_type IN ('SALE','POST','SHOP','COMMENT')),
    CONSTRAINT ck_reaction_kind CHECK(kind IN ('LIKE','BOOKMARK'))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--CREATE INDEX ix_reaction_target ON content_reactions(target_type, target_id, kind);

-- 18. engagement_targets 뷰
CREATE OR REPLACE VIEW engagement_targets AS
SELECT 'SALE' AS target_type, selno AS target_id, name AS title, location AS subtitle, current_price AS price, photo1 AS photo, selno AS parent_id FROM sale_listing
UNION ALL SELECT 'POST', post_id, title, SUBSTRING(content, 1, 160), NULL, NULL, post_id FROM board_posts
UNION ALL SELECT 'SHOP', shopId, shopname, address, NULL, photo, shopId FROM shops WHERE checked=TRUE
UNION ALL SELECT 'COMMENT', comment_id, SUBSTRING(c.content, 1, 100), p.title, NULL, NULL, c.post_id FROM board_comments c JOIN board_posts p ON p.post_id=c.post_id;




-- CREATE TABLE student (
--     unisno VARCHAR(50) PRIMARY KEY,
--     member_id BIGINT UNIQUE, -- 실제 회원 ID와 연결; 다른 학교의 동일 학번을 자동 합치지 않음
--     temp INT NOT NULL DEFAULT 365,
--     CONSTRAINT ck_student_temp CHECK(temp BETWEEN 0 AND 990)
-- );
--
-- CREATE TABLE shops (
--     school_id BIGINT,
--     version BIGINT NOT NULL DEFAULT 0,
--     shopId INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
--     owner_member_id BIGINT UNIQUE,
--     shopname VARCHAR(50) NOT NULL,
--     shopphone VARCHAR(50),
--     address VARCHAR(200),
--     photo VARCHAR(255),
--     latitude DECIMAL(10,7), longitude DECIMAL(10,7),
--     busino VARCHAR(50),
--     ceoname VARCHAR(50),
--     busi_kind VARCHAR(50),
--     bmenu VARCHAR(400),
--     reservtime VARCHAR(100),
--     busitime VARCHAR(100),
--     offday VARCHAR(20),
--     specialevent VARCHAR(50),
--     sizeandtype VARCHAR(200),
--     status VARCHAR(20) NOT NULL DEFAULT '영업종료',
--     availablespace VARCHAR(50),
--     checked BOOLEAN NOT NULL DEFAULT FALSE,
--     CONSTRAINT ck_shop_coordinates CHECK ((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)),
--     CONSTRAINT ck_shop_status CHECK (status IN ('영업중','혼잡','영업종료','휴무'))
-- );
--
-- CREATE TABLE sales (
--     school_id BIGINT,
--     selno BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
--     seller VARCHAR(50) NOT NULL,
--     seller_member_id BIGINT,
--     category VARCHAR(50) NOT NULL,
--     name VARCHAR(150) NOT NULL,
--     oprice INT NOT NULL, -- 표시가 / 할인 전 가격
--     buyprice INT, -- 판매자가 구입했던 가격 (선택)
--     wishprice INT, -- 판매자의 희망 매가 (선택, 할인 계산 기준과 별도)
--     detail VARCHAR(1500),
--     discount INT NOT NULL DEFAULT 0, -- 할인율 %, 0이면 할인 없음
--     starttime TIMESTAMP,
--     endtime TIMESTAMP,
--     regtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     status VARCHAR(20) NOT NULL DEFAULT '판매중',
--     photo1 VARCHAR(500), photo2 VARCHAR(500), photo3 VARCHAR(500),
--     location VARCHAR(80) NOT NULL DEFAULT '인하대학교',
--     icon VARCHAR(20) NOT NULL DEFAULT 'book',
--     seller_temperature DECIMAL(5,2) NOT NULL DEFAULT 36.50,
--     bumped_at TIMESTAMP,
--     CONSTRAINT fk_sale_seller_member FOREIGN KEY(seller_member_id) REFERENCES student(member_id),
--     CONSTRAINT ck_sale_price CHECK (oprice >= 0 AND (buyprice IS NULL OR buyprice >= 0) AND (wishprice IS NULL OR wishprice >= 0)),
--     CONSTRAINT ck_sale_discount CHECK (discount BETWEEN 0 AND 99),
--     CONSTRAINT ck_sale_status CHECK (status IN ('판매중','예약중','거래완료')),
--     CONSTRAINT ck_sale_discount_time CHECK (
--         (discount = 0 AND starttime IS NULL AND endtime IS NULL)
--         OR (discount BETWEEN 1 AND 99 AND starttime IS NOT NULL AND endtime IS NOT NULL
--             AND endtime = DATEADD('HOUR', 24, starttime))
--     ),
--     CONSTRAINT ck_sale_discount_amount CHECK (discount = 0 OR FLOOR(CAST(oprice AS DECIMAL(18,2)) * (100 - discount) / 100) >= 1)
-- );
-- CREATE INDEX ix_sales_status_regtime ON sales(status, regtime);
-- CREATE INDEX ix_sales_status_bumped ON sales(status, bumped_at);
-- CREATE INDEX ix_sales_seller ON sales(seller);
-- CREATE INDEX ix_sales_seller_member ON sales(seller_member_id);
--
-- CREATE TABLE board_categories (
--     category_id INT PRIMARY KEY,
--     category_name VARCHAR(30) NOT NULL UNIQUE
-- );
-- CREATE TABLE board_posts (
--     school_id BIGINT,
--     post_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
--     category_id INT NOT NULL,
--     writer VARCHAR(50) NOT NULL,
--     author_member_id BIGINT,
--     author_name VARCHAR(20),
--     school_name VARCHAR(100),
--     title VARCHAR(150) NOT NULL,
--     content TEXT NOT NULL,
--     regtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     CONSTRAINT fk_post_category FOREIGN KEY(category_id) REFERENCES board_categories(category_id)
-- );
-- CREATE INDEX ix_posts_category_time ON board_posts(category_id, regtime);
--
-- CREATE TABLE board_comments (
--     comment_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
--     post_id BIGINT NOT NULL,
--     member_id BIGINT NOT NULL,
--     author_name VARCHAR(20) NOT NULL,
--     school_name VARCHAR(100),
--     content VARCHAR(1000) NOT NULL,
--     regtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     CONSTRAINT fk_comment_post FOREIGN KEY(post_id) REFERENCES board_posts(post_id) ON DELETE CASCADE
-- );
-- CREATE INDEX ix_comments_post_time ON board_comments(post_id, comment_id);
--
-- -- 기존 화면의 약속 확정 가격을 판매글 가격과 분리해 보존할 테이블입니다.
-- CREATE TABLE trade_orders (
--     order_id VARCHAR(64) PRIMARY KEY,
--     selno BIGINT NOT NULL,
--     buyer VARCHAR(50) NOT NULL,
--     agreed_price INT NOT NULL CHECK(agreed_price >= 0),
--     status VARCHAR(30) NOT NULL DEFAULT 'RESERVED',
--     meeting_place VARCHAR(150),
--     meeting_time TIMESTAMP,
--     latitude DECIMAL(10,7), longitude DECIMAL(10,7),
--     payment_key VARCHAR(300),
--     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     CONSTRAINT fk_order_sale FOREIGN KEY(selno) REFERENCES sales(selno),
--     CONSTRAINT ck_order_status CHECK(status IN ('RESERVED','PAYMENT_READY','WAITING_FOR_DEPOSIT','PAID','COMPLETED','거래완료','CANCELED')),
--     CONSTRAINT ck_order_coordinates CHECK ((latitude IS NULL AND longitude IS NULL) OR
--         (latitude IS NOT NULL AND longitude IS NOT NULL AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180))
-- );
-- CREATE INDEX ix_orders_sale ON trade_orders(selno);
--
-- -- 거래당 각 당사자가 상대방을 한 번 평가합니다.
-- CREATE TABLE manner_ratings (
--     rating_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
--     order_id VARCHAR(64) NOT NULL,
--     reviewer_unisno VARCHAR(50) NOT NULL,
--     recipient_unisno VARCHAR(50) NOT NULL,
--     feedback VARCHAR(10) NOT NULL,
--     delta INT NOT NULL,
--     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     CONSTRAINT fk_rating_order FOREIGN KEY(order_id) REFERENCES trade_orders(order_id),
--     CONSTRAINT fk_rating_reviewer FOREIGN KEY(reviewer_unisno) REFERENCES student(unisno),
--     CONSTRAINT fk_rating_recipient FOREIGN KEY(recipient_unisno) REFERENCES student(unisno),
--     CONSTRAINT uq_rating_order_reviewer UNIQUE(order_id,reviewer_unisno),
--     CONSTRAINT ck_rating_feedback CHECK(feedback IN ('GOOD','BAD')),
--     CONSTRAINT ck_rating_self CHECK(reviewer_unisno <> recipient_unisno)
-- );
--
-- -- 실시간 할인 가격을 계산합니다. 만료 후 저장 값을 갱신하지 않아도 조회 가격은 복구됩니다.
-- CREATE VIEW sale_listing AS
-- SELECT s.school_id,s.selno,s.seller,s.seller_member_id,s.category,s.name,s.oprice,s.buyprice,s.wishprice,s.detail,s.discount,s.starttime,s.endtime,s.regtime,s.status,s.photo1,s.photo2,s.photo3,s.location,s.icon,s.bumped_at,
--     COALESCE(CAST(st.temp AS DECIMAL(5,1))/10,s.seller_temperature) AS seller_temperature,
--     CAST(CASE WHEN status = '판매중' AND discount > 0 AND starttime <= CURRENT_TIMESTAMP AND endtime > CURRENT_TIMESTAMP
--         THEN FLOOR(CAST(oprice AS DECIMAL(18,2)) * (100 - discount) / 100) ELSE oprice END AS INT) AS current_price,
--     CASE WHEN status = '거래완료' THEN 4 WHEN status = '예약중' THEN 3
--          WHEN regtime > DATEADD('HOUR', -24, CURRENT_TIMESTAMP) THEN 0
--          WHEN bumped_at IS NOT NULL AND bumped_at > DATEADD('HOUR', -24, CURRENT_TIMESTAMP) THEN 1
--          ELSE 2 END AS exposure_priority
-- FROM sales s LEFT JOIN student st ON st.unisno=s.seller AND (s.seller_member_id IS NULL OR st.member_id=s.seller_member_id);
--
-- -- 기존 Product.java / ProductMapper.java의 조회 컬럼을 유지하는 호환 VIEW입니다.
-- CREATE VIEW products AS
-- SELECT selno AS id, name AS title, current_price AS price, category, location, icon FROM sale_listing;
--
--
-- -- Record only successful owner/manager edits; keep actor snapshots after manager deletion.
-- CREATE TABLE shop_change_logs (
--     change_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
--     shop_id INT NOT NULL,
--     actor_member_id BIGINT NOT NULL,
--     actor_name VARCHAR(20) NOT NULL,
--     actor_role VARCHAR(20) NOT NULL,
--     field_key VARCHAR(40) NOT NULL,
--     field_label VARCHAR(50) NOT NULL,
--     before_value VARCHAR(500) NOT NULL,
--     after_value VARCHAR(500) NOT NULL,
--     changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     CONSTRAINT fk_shop_change_shop FOREIGN KEY(shop_id) REFERENCES shops(shopId)
-- );
-- CREATE INDEX ix_shop_change_time ON shop_change_logs(shop_id,changed_at,change_id);
--
-- CREATE TABLE admin_action_logs (
--     action_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
--     shop_id INT NOT NULL,
--     shop_name VARCHAR(50) NOT NULL,
--     actor_member_id BIGINT NOT NULL,
--     actor_name VARCHAR(20) NOT NULL,
--     action VARCHAR(20) NOT NULL,
--     before_value VARCHAR(100) NOT NULL,
--     after_value VARCHAR(100) NOT NULL,
--     note VARCHAR(300) NOT NULL,
--     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     CONSTRAINT fk_admin_action_shop FOREIGN KEY(shop_id) REFERENCES shops(shopId)
-- );
-- CREATE INDEX ix_admin_action_time ON admin_action_logs(created_at,action_id);
--
-- -- v14: reservation notification outbox (all delivery is disabled by default).
-- CREATE TABLE seller_notifications (
--  notification_id VARCHAR(36) PRIMARY KEY,
--  order_id VARCHAR(64) NOT NULL,
--  seller_member_id BIGINT NOT NULL,
--  sale_id BIGINT NOT NULL,
--  title VARCHAR(150) NOT NULL,
--  message VARCHAR(1500) NOT NULL,
--  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--  read_at TIMESTAMP,
--  CONSTRAINT fk_notification_order FOREIGN KEY(order_id) REFERENCES trade_orders(order_id),
--  CONSTRAINT uq_notification_order_recipient UNIQUE(order_id,seller_member_id)
-- );
-- CREATE INDEX ix_notification_seller ON seller_notifications(seller_member_id,created_at);
-- CREATE TABLE notification_deliveries (
--  delivery_id VARCHAR(36) PRIMARY KEY,
--  notification_id VARCHAR(36) NOT NULL,
--  channel VARCHAR(10) NOT NULL,
--  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
--  attempted_at TIMESTAMP,
--  last_error VARCHAR(300),
--  CONSTRAINT uq_notification_channel UNIQUE(notification_id,channel),
--  CONSTRAINT ck_notification_channel CHECK(channel IN ('SMS','EMAIL','KAKAO')),
--  CONSTRAINT fk_delivery_notification FOREIGN KEY(notification_id) REFERENCES seller_notifications(notification_id)
-- );
-- CREATE INDEX ix_notification_pending ON notification_deliveries(status);
-- CREATE TABLE notification_settings (
--  member_id BIGINT PRIMARY KEY,
--  email VARCHAR(100),phone VARCHAR(20),
--  sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
--  email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
--  kakao_enabled BOOLEAN NOT NULL DEFAULT TRUE
-- );
--
-- -- Teammate five-score manner review table. Existing student/member tables stay unchanged.
-- CREATE TABLE IF NOT EXISTS trade_reviews (
--                                              review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
--                                              order_id VARCHAR(50) NOT NULL,
--     reviewer_id VARCHAR(50) NOT NULL,
--     target_id VARCHAR(50) NOT NULL,
--     score INT NOT NULL,
--     attitude VARCHAR(50),
--     content VARCHAR(500),
--     temperature_delta INT NOT NULL,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     CONSTRAINT uk_trade_review UNIQUE (order_id, reviewer_id)
--     );
--
-- -- Likes and private bookmarks
-- -- Additive migration for existing H2/MySQL databases. Keep existing content intact.
-- CREATE TABLE IF NOT EXISTS content_reactions (
--     member_id BIGINT NOT NULL,
--     target_type VARCHAR(10) NOT NULL,
--     target_id BIGINT NOT NULL,
--     kind VARCHAR(10) NOT NULL,
--     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     PRIMARY KEY(member_id,target_type,target_id,kind),
--     CONSTRAINT ck_reaction_type CHECK(target_type IN ('SALE','POST','SHOP','COMMENT')),
--     CONSTRAINT ck_reaction_kind CHECK(kind IN ('LIKE','BOOKMARK'))
-- );
-- CREATE INDEX IF NOT EXISTS ix_reaction_target ON content_reactions(target_type,target_id,kind);
-- CREATE OR REPLACE VIEW engagement_targets AS
--  SELECT 'SALE' AS target_type,selno AS target_id,name AS title,location AS subtitle,current_price AS price,photo1 AS photo,selno AS parent_id,school_id FROM sale_listing
--  UNION ALL SELECT 'POST',post_id,title,SUBSTRING(content,1,160),NULL,NULL,post_id,school_id FROM board_posts
--  UNION ALL SELECT 'SHOP',shopId,shopname,address,NULL,photo,shopId,school_id FROM shops WHERE checked=TRUE
--  UNION ALL SELECT 'COMMENT',comment_id,SUBSTRING(c.content,1,100),p.title,NULL,NULL,c.post_id,p.school_id FROM board_comments c JOIN board_posts p ON p.post_id=c.post_id;

CREATE TABLE IF NOT EXISTS catalog_meta(name VARCHAR(60) PRIMARY KEY);
CREATE TABLE IF NOT EXISTS business_categories(category_id BIGINT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(50) NOT NULL UNIQUE,version BIGINT NOT NULL DEFAULT 0);
