-- 공개 시연 계정입니다. 실제 로그인 정보가 아닙니다.
INSERT INTO student(unisno) VALUES ('demo-buyer'),('demo-seller');

-- 실제 사업자/비밀번호/결제키가 아닌 초기 시연 데이터입니다.
INSERT INTO shops (shopname,shopphone,busi_kind,bmenu,reservtime,busitime,offday,specialevent,sizeandtype,status,availablespace,checked,reputation)
VALUES ('캠퍼스 카페 (예시)','032-000-0000','카페','아메리카노 3,000원 / 라떼 4,000원','전화 예약 / 2~6명','09:00~21:00','일요일','학생 할인 예시','4인석 3개 / 입식','영업중','4인석 1개',FALSE,'검증 전 시연 가게');

INSERT INTO board_categories(category_id,category_name) VALUES (1,'학교'),(2,'주변'),(3,'취업팁'),(4,'고민상담'),(5,'제안');
INSERT INTO board_posts(category_id,writer,title,content) VALUES
(1,'demo-student','전공책 도서관 비치 여부 질문','첨부 표의 학교 게시판 예시입니다.'),
(2,'demo-student','후문 근처 조용한 카페 추천','첨부 표의 주변 게시판 예시입니다.'),
(3,'demo-student','면접 준비 팁을 나눠요','개인정보 없이 작성한 취업팁 예시입니다.');

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
