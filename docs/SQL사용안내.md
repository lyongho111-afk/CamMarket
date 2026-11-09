# SQL 사용 안내

기본 실행은 H2 메모리 DB입니다. schema.sql → data.sql이 서버 시작 시 실행되고, XML Mapper가 모든 조회·변경 SQL을 담당합니다.

- 실행 SQL: src/main/resources/schema.sql / data.sql
- Mapper: src/main/resources/mappers/SalesDao.xml / BoardDao.xml / ShopDao.xml / TradeDao.xml
- DTO.currentPrice: 조회 VIEW의 현재 가격입니다. DTO.oprice는 할인 전 원가이며 discount/endtime으로 자동 복구를 계산합니다.
- 예약의 agreed_price는 서버에서 현재 가격을 읽어 저장하며 상품 원가·할인 만료와 분리합니다.
- JSON Controller도 Service → DAO → XML로 연결하며 카드 화면이 이를 사용합니다.
- docs/sql의 MySQL 자료는 참고용입니다. 실제 MySQL 전환 시 드라이버·접속 정보와 XML 날짜 함수를 맞춰야 합니다.

메모리 DB이므로 서버 종료 후에는 예시로 초기화됩니다. SQL 파일을 별도 DB에 무작정 실행하지 말고 사용하는 DB 종류를 먼저 확인하세요.
