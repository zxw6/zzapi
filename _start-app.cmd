@echo off
setlocal
cd /d "%~dp0"

set "HOME=e:\project-ai"
set "USERPROFILE=e:\project-ai"
set "MAVEN_USER_HOME=e:\project-ai\.m2home"
set "JAVA_HOME=D:\JAVA\JDK\jdk17"
set "PATH=%JAVA_HOME%\bin;%PATH%"

start "zxw-app" /b cmd /c "java -jar target\Test-0.0.1-SNAPSHOT.jar > run-app.new.out.log 2> run-app.new.err.log"
exit /b 0
