@echo off
setlocal

cd /d "%~dp0"

set "HOME=e:\project-ai"
set "USERPROFILE=e:\project-ai"
set "MAVEN_USER_HOME=e:\project-ai\.m2home"
set "JAVA_HOME=D:\JAVA\JDK\jdk17"
set "PATH=%JAVA_HOME%\bin;%PATH%"

call .\mvnw.cmd -q -DskipTests package
if errorlevel 1 exit /b 1

exit /b 0
