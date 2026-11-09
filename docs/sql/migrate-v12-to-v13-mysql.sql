-- 수동 적용용: v12 스키마에 한 번만 실행합니다. 실행 전 DB를 백업하세요.
-- 기본 메모리 H2 시연 실행에는 필요하지 않습니다.
ALTER TABLE shops ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE admin_action_logs (
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
);
CREATE INDEX ix_admin_action_time ON admin_action_logs(created_at,action_id);
