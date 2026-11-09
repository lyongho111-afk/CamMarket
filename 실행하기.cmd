@echo off
chcp 65001 >nul
cd /d "%~dp0"
if exist "%~dp0config\local.env.cmd" call "%~dp0config\local.env.cmd"
set "CAMPUS_JAVA=%~dp0app\runtime\bin\java.exe"
if not exist "%CAMPUS_JAVA%" (echo Included Java runtime is missing. Extract the entire ZIP first. & pause & exit /b 1)
echo Campus re:market - open http://localhost:8080/ after startup.
echo To stop, press Ctrl+C in this window.
"%CAMPUS_JAVA%" -Duser.timezone=Asia/Seoul -jar "%~dp0app\campus-remarket.jar"
if errorlevel 1 pause
