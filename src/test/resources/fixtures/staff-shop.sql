-- Staff tests own a separate fixture; sample shops are not part of the shipped catalog.
INSERT INTO shops(owner_member_id,shopname,shopphone,busi_kind,bmenu,busitime,status,checked,address,latitude,longitude)
SELECT id,'운영 테스트 가게','032-000-0000','카페','커피 3000원','09:00~21:00','영업중',TRUE,'테스트 전용 주소',37.4495,126.6570
FROM member WHERE account_key='staff:owner1';

UPDATE shops SET school_id=(SELECT school_id FROM member WHERE member.id=shops.owner_member_id) WHERE school_id IS NULL;
