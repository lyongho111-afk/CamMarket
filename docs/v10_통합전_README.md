# v10 매너온도 평가 반영 (2026-10-30)

Windows: ZIP 전체 압축 해제 후 **실행하기.cmd**를 실행하고 http://localhost:8080/ 을 여세요.
`student.unisno` 문자열, `temp` 정수 기본365, 거래 COMPLETED/거래완료 후 상대 평가를 추가했습니다.
완료 상품 상세 또는 `/trades/selectOne?id=demo-completed-1`에서 확인할 수 있습니다.
로그인은 현재 demo-buyer 시연이며 실제 로그인 담당의 세션 연결이 필요합니다.
증감 정책은 임시 +0.1/-0.1도이며 설정으로 변경할 수 있습니다.
**조원에게 전달할 상세 계약: docs/조원별_협업_인터페이스_v10.md**

아래는 유지되는 v9 기능 설명입니다.

# 캠퍼스 리마켓 — 카드형 화면 + MyBatis 연결본

## 바로 실행하기 (Windows 64비트)
1. ZIP 전체를 새 폴더에 압축 해제합니다. ZIP 안에서 바로 실행하지 마세요.
2. `실행하기.cmd`를 실행합니다. Windows용 Java 25 실행 환경과 빌드된 JAR를 포함했습니다.
3. 콘솔에 Started CampusApplication이 표시되면 http://localhost:8080/ 에 접속합니다. `화면열기.url`로도 열 수 있습니다.
4. 실행 창에서 Ctrl+C를 누르면 종료합니다. 이 예제는 메모리 H2이므로 서버 종료 시 변경 내용이 초기화됩니다. 브라우저 새로고침만으로 초기화되지는 않습니다.

외부 공유주소는 제공자의 실행 컴퓨터와 터널이 켜져 있을 때만 열립니다. 다른 컴퓨터에서 ZIP을 실행할 때는 그 컴퓨터의 localhost 주소를 사용합니다.

## IntelliJ에서 소스 수정
- ZIP의 프로젝트 폴더 또는 build.gradle을 Open하고 Gradle 프로젝트로 가져옵니다.
- Project SDK / Gradle JVM: JDK 25. 소스 빌드에는 별도 JDK 25가 필요합니다. 포함한 app/runtime은 실행용입니다.
- src/main/java/com/campus/remarket/CampusApplication.java의 main을 실행합니다.
- Windows: gradlew.bat bootRun / macOS·Linux: sh gradlew bootRun
- 빌드·테스트: gradlew.bat test bootJar. 최초 Gradle 의존성 다운로드에는 인터넷이 필요합니다.

## 메인 화면과 실제 연결
- `/`, `/sales/selectAll`: 기존 카드형 메인 화면. 별도의 표 화면이나 기존 시안 선택 과정 없이 바로 열립니다.
- 중고거래: 검색, 카테고리, 가격 정렬, 할인만 보기, 상세 보기, 판매글 등록.
- 내 판매글: 실제 등록 개수, 판매글 수정·삭제, 할인율 자동 계산, 24시간 할인 시작·종료, 끌어올리기.
- 상품 상세 → 거래 약속: 서버 현재 가격으로 예약하며 DB에 약속 가격을 별도 보관합니다.
- 지도·결제: Google 지도, 거래 장소 변경·저장, 예약 취소, 대면 거래 완료, 설정 시 토스 테스트 카드 결제창.
- 동네생활: 학교·주변·취업팁·고민상담·제안 분류, 글 등록·상세·수정·삭제.
- 가게: 왼쪽 Google 지도와 오른쪽 ‘공강에 들르기 좋은 곳’ 목록으로 구성합니다. 영업 상태·시간·메뉴 등 공개 정보만 조회하며 상태 변경·가게 편집 폼은 학생 화면에 없습니다. `/shops/selectAll`도 이 학생용 화면으로 이동합니다.
- 모임 탭을 제거했습니다. 상단 결제 탭은 없으며 상품 상세에서 지도·결제로 진입합니다.
- 찜은 브라우저 세션에서만 유지됩니다. 실사용자 로그인·판매자 권한·채팅·댓글은 구현 범위에 포함하지 않습니다.

## 요청한 작성 순서
DTO.java → DAO.java → Mapper.xml(SQL) → Service.java → Controller.java → 화면(.html)

판매글 기준:
1. src/main/java/com/campus/remarket/dto/SalesDto.java
2. src/main/java/com/campus/remarket/dao/SalesDao.java
3. src/main/resources/mappers/SalesDao.xml
4. src/main/java/com/campus/remarket/service/SalesService.java
5. src/main/java/com/campus/remarket/controller/ProductApiController.java (카드 화면 JSON 요청), SalesController.java (화면 URL 및 학습용 폼)
6. src/main/resources/templates/market/index.html (메인 화면)
7. src/main/resources/static/preview/app.js (화면 버튼 → Controller 요청), features.js (표시 함수), integrations-live.js (지도·토스 SDK)

화면 HTML과 CSS는 기존 디자인을 재사용하며, static/preview라는 자산 폴더명과 관계없이 메인 페이지의 버튼이 실제 Java Service에 연결됩니다. Java DAO에 SQL 어노테이션을 넣지 않고 XML Mapper에 SQL을 모았습니다. Board·Shop·Trade도 같은 계층 구조입니다.

## SQL
- src/main/resources/schema.sql: 실행용 H2 테이블·제약·조회 VIEW
- src/main/resources/data.sql: 공개 가능한 초기 예시 데이터
- src/main/resources/mappers/*.xml: SELECT/INSERT/UPDATE/DELETE 및 할인·끌어올리기·거래 SQL
- docs/sql: 참고 MySQL 자료. 기본 실행 DB는 H2이며, MySQL로 바꿀 때는 드라이버·접속정보·DATEADD 등의 날짜 함수도 함께 바꿔야 합니다.
- H2 콘솔은 로컬 학습용으로 기본 활성화됩니다. 외부 공유 실행에서는 비활성화했습니다.

## 지도와 토스 설정
Google 키가 없을 때도 Google 지도 임베드와 위치 링크를 제공합니다. 지도 안에서 직접 좌표를 선택하려면 GOOGLE_MAPS_API_KEY를 설정하고 Maps JavaScript API 사용 및 도메인 제한을 맞춰야 합니다.

토스는 발급한 API 개별 연동 테스트 키 `test_ck_...` / `test_sk_...`가 필요합니다. `config/local.env.example.cmd`를 복사해 `config/local.env.cmd`로 저장한 뒤 본인 키를 입력하면 실행하기.cmd가 읽습니다. IntelliJ는 Run Configuration의 환경변수에 같은 이름을 설정하세요. 시크릿 키는 HTML/JS/공개 설정 API로 전송하지 않습니다. 발급한 키가 없으므로 이번 전달본에서 실제 토스 결제창·승인 성공은 검증하지 않았습니다. 미설정 시 결제 버튼은 비활성화되고 필요한 설정을 안내합니다.

테스트 카드 결제는 예약 DB의 agreed_price와 주문번호로 준비하며, 서버가 토스 승인 응답의 주문번호·금액·DONE 상태를 검증한 후 PAID로 기록합니다. 대면 거래 완료는 COMPLETED 처리이며 결제 승인·송금으로 표시하지 않습니다. 실결제, 환불, 정산, 가상계좌 입금 웹훅은 구현 범위 밖입니다.
공식 참고: https://docs.tosspayments.com/guides/v2/get-started/payment-flow / https://docs.tosspayments.com/reference

## 구현 규칙
할인율 1~99% 정수, 할인 후 최소 1원, 1원 미만 버림. 24시간 후 자동 원가 복구. 할인 중 가격 변경·시간 연장 불가, 조기 종료 가능. 예약 가격은 할인 만료 후에도 유지합니다.
기본 순서: 최신(24시간) → 끌어올리기(24시간) → 판매 중(온도 내림차순) → 예약 중 → 거래 완료. 끌어올리기 재사용 간격 24시간. 예약·완료 글의 할인과 끌어올리기를 제한합니다. 거래 기록이 있는 글은 삭제를 제한합니다.
공개 시연은 demo-seller / demo-buyer 예시 계정으로 진행합니다. 운영 서비스를 위한 사용자 인증·권한 확인이 갖춰진 버전은 아닙니다.

## 포함 자료
전체 소스, Gradle Wrapper, H2 SQL, Windows 실행 JAR·런타임, 계획서(HTML/Markdown), 작성 순서 안내, 테스트 결과를 포함합니다. 개인 PC의 .idea/.iml 경로, 빌드 캐시, 실제 비밀 키는 포함하지 않습니다.


## 학생 화면 수정 (2026-10-29, v9)
1. 배너 아래 중고거래·동네생활 바로가기 바 제거.
2. 가게 탭은 왼쪽 ‘캠퍼스 주변 지도’, 오른쪽 ‘공강에 들르기 좋은 곳’ 패널로 구성. 좁은 화면에서는 지도 아래 목록 배치. 지도에서 캠퍼스·카페·식당을 선택해 살펴볼 수 있습니다.
3. 카페 상태 변경 시연·반영·유효 시간·관리용 편집 화면 제거. 학생은 영업 상태·영업시간·메뉴 등만 확인합니다. 사장·관리자 페이지는 이번 범위에 추가하지 않았습니다.

학생용 가게 조회의 소스 흐름: ShopDto.java → ShopDao.java → ShopDao.xml(selectAll/selectOne) → ShopService.java → ShopApiController.java/ShopController.java → templates/market/index.html. UI 구성은 static/preview/shops.js 및 shops.css입니다. ShopDao/Service의 작성 메서드는 첨부 SQL 학습용으로 보존하지만 가게 작성·상태 변경 HTTP 경로는 학생용 Controller에서 제공하지 않습니다.
