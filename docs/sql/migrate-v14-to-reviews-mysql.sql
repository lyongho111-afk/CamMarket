-- Apply once to an existing v14 database; original data is retained.
CREATE TABLE IF NOT EXISTS trade_reviews (
                                             review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             order_id VARCHAR(50) NOT NULL,
    reviewer_id VARCHAR(50) NOT NULL,
    target_id VARCHAR(50) NOT NULL,
    score INT NOT NULL,
    attitude VARCHAR(50),
    content VARCHAR(500),
    temperature_delta INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_trade_review UNIQUE (order_id, reviewer_id)
    );
