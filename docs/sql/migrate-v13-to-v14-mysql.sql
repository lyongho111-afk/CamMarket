-- v13 -> v14. Back up your database first. Run once, manually.
ALTER TABLE shops DROP COLUMN ceoid;
ALTER TABLE shops DROP COLUMN ceopw;
ALTER TABLE shops DROP COLUMN man1id;
ALTER TABLE shops DROP COLUMN man1pw;
ALTER TABLE shops DROP COLUMN man1name;
ALTER TABLE shops DROP COLUMN man2id;
ALTER TABLE shops DROP COLUMN man2pw;
ALTER TABLE shops DROP COLUMN man2name;
ALTER TABLE shops DROP COLUMN man3id;
ALTER TABLE shops DROP COLUMN man3pw;
ALTER TABLE shops DROP COLUMN man3name;
ALTER TABLE shops DROP COLUMN reputation;
ALTER TABLE shops ADD COLUMN address VARCHAR(200);
ALTER TABLE shops ADD COLUMN latitude DECIMAL(10,7);
ALTER TABLE shops ADD COLUMN longitude DECIMAL(10,7);
ALTER TABLE shops ADD CONSTRAINT ck_shop_coordinates CHECK ((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180));
INSERT INTO board_categories(category_id,category_name) VALUES (6,'맛집 추천'),(7,'술집 추천'),(8,'가성비'),(9,'공지'),(10,'자유게시판');
-- v14: reservation notification outbox (all delivery is disabled by default).
CREATE TABLE seller_notifications (
 notification_id VARCHAR(36) PRIMARY KEY,
 order_id VARCHAR(64) NOT NULL UNIQUE,
 seller_member_id BIGINT NOT NULL,
 sale_id BIGINT NOT NULL,
 title VARCHAR(150) NOT NULL,
 message VARCHAR(1500) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 read_at TIMESTAMP,
 CONSTRAINT fk_notification_order FOREIGN KEY(order_id) REFERENCES trade_orders(order_id)
);
CREATE INDEX ix_notification_seller ON seller_notifications(seller_member_id,created_at);
CREATE TABLE notification_deliveries (
 delivery_id VARCHAR(36) PRIMARY KEY,
 notification_id VARCHAR(36) NOT NULL,
 channel VARCHAR(10) NOT NULL,
 status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
 attempted_at TIMESTAMP,
 last_error VARCHAR(300),
 CONSTRAINT uq_notification_channel UNIQUE(notification_id,channel),
 CONSTRAINT ck_notification_channel CHECK(channel IN ('SMS','EMAIL','KAKAO')),
 CONSTRAINT fk_delivery_notification FOREIGN KEY(notification_id) REFERENCES seller_notifications(notification_id)
);
CREATE INDEX ix_notification_pending ON notification_deliveries(status);
CREATE TABLE notification_settings (
 member_id BIGINT PRIMARY KEY,
 email VARCHAR(100),phone VARCHAR(20),
 sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 kakao_enabled BOOLEAN NOT NULL DEFAULT TRUE
);
