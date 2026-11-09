-- 공개 시연 계정입니다. 실제 로그인 정보가 아닙니다.
INSERT INTO student(unisno) VALUES ('demo-buyer'),('demo-seller');

-- 실제 사업자/비밀번호/결제키가 아닌 초기 시연 데이터입니다.
INSERT INTO shops (shopname,shopphone,busi_kind,bmenu,reservtime,busitime,offday,specialevent,sizeandtype,status,availablespace,checked,address,latitude,longitude)
VALUES ('캠퍼스 카페 (예시)','032-000-0000','카페','아메리카노 3,000원 / 라떼 4,000원','전화 예약 / 2~6명','09:00~21:00','일요일','학생 할인 예시','4인석 3개 / 입식','영업중','4인석 1개',FALSE,'인하대학교 인근 · 예시 위치',37.4495,126.6570);

INSERT INTO board_categories(category_id,category_name) VALUES (1,'학교'),(2,'주변'),(3,'취업팁'),(4,'고민상담'),(5,'제안'),(6,'맛집 추천'),(7,'술집 추천'),(8,'가성비'),(9,'공지'),(10,'자유게시판');
INSERT INTO board_posts(category_id,writer,title,content) VALUES
(6,'demo-student','후문 근처 점심 맛집 추천해 주세요','수업 사이에 빠르게 먹을 수 있는 식당을 추천해 주세요. 예시 게시글입니다.'),
(7,'demo-student','친구와 가기 좋은 조용한 술집 있나요?','학교 주변에서 대화하기 편한 곳을 찾고 있어요. 예시 게시글입니다.'),
(8,'demo-student','만원 이하 든든한 한 끼를 찾아요','학생 할인이나 가성비 메뉴 정보를 같이 나눠요. 예시 게시글입니다.'),
(9,'demo-student','커뮤니티 이용 안내','광고·개인정보 노출을 삼가고 서로 존중해 주세요. 예시 안내글입니다.');

INSERT INTO sales(seller,category,name,oprice,detail,location,icon,regtime,seller_temperature,status) VALUES
('demo-seller','노트북·디지털','아이패드 에어 · 필기용 추천',280000,'필기용 태블릿 예시','학생회관','laptop',DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -8 DAY),46.7,'거래완료'),
('demo-seller','제도·자격증','스테들러 제도용 샤프 & 삼각자',12000,'제도 도구 예시','정문','tools',DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -7 DAY),37.8,'예약중'),
('demo-seller','조리·실습복','한식조리 실습용 조리복 · L',15000,'조리복 예시','후문','chef',DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -6 DAY),43.2,'판매중'),
('demo-seller','전공서적','건축설계 기초 전공책 2권',22000,'전공책 예시','도서관','book',DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -5 DAY),48.6,'판매중'),
('demo-seller','노트북·디지털','LG 그램 14인치 · 충전기 포함',420000,'노트북 예시','인하대역','laptop',DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -4 DAY),39.4,'판매중'),
('demo-seller','제도·자격증','실내건축산업기사 제도도구 세트',45000,'제도 도구 예시','공대 2호관','tools',DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -3 DAY),45.1,'판매중'),
('demo-seller','조리·실습복','조리 실습복 세트 · M 사이즈',25000,'실습복 예시','정문','chef',DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -4 HOUR),38.2,'판매중'),
('demo-seller','전공서적','자료구조 & 알고리즘 전공서적',18000,'전공책 예시','후문','book',DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -2 HOUR),42.8,'판매중');
UPDATE sales SET bumped_at=DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -3 HOUR) WHERE selno=5;
UPDATE sales SET bumped_at=DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 HOUR),discount=20,starttime=CURRENT_TIMESTAMP,endtime=DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 24 HOUR) WHERE selno=6;
UPDATE sales SET discount=20,starttime=CURRENT_TIMESTAMP,endtime=DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 24 HOUR) WHERE selno=8;
INSERT INTO trade_orders(order_id,selno,buyer,agreed_price,status,meeting_place) VALUES
('demo-completed-1',1,'demo-buyer',280000,'COMPLETED','학생회관'),
('demo-reserved-2',2,'demo-buyer',12000,'RESERVED','인하대학교 정문');
