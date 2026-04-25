@echo off
setlocal
cd /d "%~dp0"

set "HOME=e:\project-ai"
set "USERPROFILE=e:\project-ai"
set "MAVEN_USER_HOME=e:\project-ai\.m2home"
set "JAVA_HOME=D:\JAVA\JDK\jdk17"
set "M2_HOME=e:\project-ai\.m2home\wrapper\dists\apache-maven-3.9.14\ed7edd442f634ac1c1ef5ba2b61b6d690b5221091f1a8e1123f5fadcc967520d"
set "PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%"

call mvn.cmd -DskipTests package
exit /b %errorlevel%
