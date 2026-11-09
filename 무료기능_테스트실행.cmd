@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo 캠퍼스 리마켓 - 무료 로컬 테스트
echo 학생: http://127.0.0.1:8091/sales/selectAll
echo 사장: http://127.0.0.1:8091/owner/
echo 관리자: http://127.0.0.1:8091/admin/
echo 테스트 수신함: http://127.0.0.1:8091/local-demo/inbox
echo 실제 메일, 문자, 카카오톡은 발송하지 않습니다.
echo 테스트 데이터는 서버 종료 후 초기화됩니다.
if not exist "app\runtime\bin\java.exe" (
 echo ZIP 전체를 새 폴더에 풀고 실행해 주세요.
 pause
 exit /b 1
)
"app\runtime\bin\java.exe" -jar "app\campus-remarket.jar" --spring.profiles.active=local-demo --server.port=8091
pause
