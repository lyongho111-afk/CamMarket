@echo off
chcp 65001 >nul
cd /d "%~dp0"
set "CAMPUS_JAVA=%~dp0app\runtime\bin\java.exe"
if not exist "%CAMPUS_JAVA%" (echo Included Java runtime is missing. Extract the whole ZIP. & pause & exit /b 1)
echo First start the shared server using 실행하기.cmd.
echo Open http://localhost:8082/ after startup.
"%CAMPUS_JAVA%" -Xmx256m -Duser.timezone=Asia/Seoul -Dloader.main=portal.campus.PortalGatewayApplication -cp "%~dp0app\campus-remarket.jar" org.springframework.boot.loader.launch.PropertiesLauncher --portal.site=admin --server.port=8082 --portal.backend=http://127.0.0.1:8080
if errorlevel 1 pause
