-- User-requested removal of this exact bundled sample only. Back up the DB first.
-- Retain the owner/member account. Real shops and other records are unaffected.
BEGIN;
DELETE FROM content_reactions WHERE target_type='SHOP' AND target_id IN (SELECT shopId FROM shops WHERE shopname='캠퍼스 카페 (예시)' AND shopphone='032-000-0000' AND address='인하대학교 인근 · 예시 위치');
DELETE FROM shop_change_logs WHERE shop_id IN (SELECT shopId FROM shops WHERE shopname='캠퍼스 카페 (예시)' AND shopphone='032-000-0000' AND address='인하대학교 인근 · 예시 위치');
DELETE FROM admin_action_logs WHERE shop_id IN (SELECT shopId FROM shops WHERE shopname='캠퍼스 카페 (예시)' AND shopphone='032-000-0000' AND address='인하대학교 인근 · 예시 위치');
DELETE FROM shops WHERE shopname='캠퍼스 카페 (예시)' AND shopphone='032-000-0000' AND address='인하대학교 인근 · 예시 위치';
COMMIT;
