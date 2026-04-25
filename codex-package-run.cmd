@echo off
setlocal

cd /d "%~dp0"

for /f "tokens=5" %%a in ('netstat -ano ^| findstr :9988 ^| findstr LISTENING') do (
    taskkill /PID %%a /F >nul 2>nul
)

if exist run-app.current.out.log del /f /q run-app.current.out.log >nul 2>nul
if exist run-app.current.err.log del /f /q run-app.current.err.log >nul 2>nul

set "HOME=e:\project-ai"
set "USERPROFILE=e:\project-ai"
set "MAVEN_USER_HOME=e:\project-ai\.m2home"
set "JAVA_HOME=D:\JAVA\JDK\jdk17"
set "PATH=%JAVA_HOME%\bin;%PATH%"

call mvnw.cmd -q -DskipTests package
if errorlevel 1 exit /b 1

start "zxw-app" /b cmd /c "cd /d %CD% && set HOME=e:\project-ai&& set USERPROFILE=e:\project-ai&& set MAVEN_USER_HOME=e:\project-ai\.m2home&& set JAVA_HOME=D:\JAVA\JDK\jdk17&& set PATH=D:\JAVA\JDK\jdk17\bin;%PATH%&& java -jar target\Test-0.0.1-SNAPSHOT.jar > run-app.current.out.log 2> run-app.current.err.log"

exit /b 0
