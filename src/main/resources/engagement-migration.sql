-- Additive migration for existing H2/MySQL databases. Keep existing content intact.
CREATE TABLE IF NOT EXISTS content_reactions (
    member_id BIGINT NOT NULL,
    target_type VARCHAR(10) NOT NULL,
    target_id BIGINT NOT NULL,
    kind VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(member_id,target_type,target_id,kind),
    CONSTRAINT ck_reaction_type CHECK(target_type IN ('SALE','POST','SHOP','COMMENT')),
    CONSTRAINT ck_reaction_kind CHECK(kind IN ('LIKE','BOOKMARK'))
);
CREATE INDEX ix_reaction_target ON content_reactions(target_type,target_id,kind);
CREATE OR REPLACE VIEW engagement_targets AS
 SELECT 'SALE' AS target_type,selno AS target_id,name AS title,location AS subtitle,current_price AS price,photo1 AS photo,selno AS parent_id FROM sale_listing
 UNION ALL SELECT 'POST',post_id,title,SUBSTRING(content,1,160),NULL,NULL,post_id FROM board_posts
 UNION ALL SELECT 'SHOP',shopId,shopname,address,NULL,photo,shopId FROM shops WHERE checked=TRUE
 UNION ALL SELECT 'COMMENT',comment_id,SUBSTRING(c.content,1,100),p.title,NULL,NULL,c.post_id FROM board_comments c JOIN board_posts p ON p.post_id=c.post_id;
