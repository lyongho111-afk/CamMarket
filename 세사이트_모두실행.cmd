@echo off
chcp 65001 >nul
cd /d "%~dp0"
if exist "%~dp0config\local.env.cmd" call "%~dp0config\local.env.cmd"
set "CAMPUS_JAVA=%~dp0app\runtime\bin\java.exe"
if not exist "%CAMPUS_JAVA%" (echo Included Java runtime is missing. Extract the whole ZIP. & pause & exit /b 1)
echo Starting student, owner and admin sites. Please wait.
"%CAMPUS_JAVA%" -Duser.timezone=Asia/Seoul -Dloader.main=portal.campus.StartSites -cp "%~dp0app\campus-remarket.jar" org.springframework.boot.loader.launch.PropertiesLauncher
pause
