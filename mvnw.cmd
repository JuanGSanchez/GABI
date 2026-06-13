@REM Maven wrapper script (Windows)
@REM Generated for GABI workstream (a).
@REM This script runs Maven using the distribution pinned in .mvn/wrapper/maven-wrapper.properties

@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0

if "%JAVA_HOME%"=="" (
  echo JAVA_HOME is not set. Please set JAVA_HOME to a JDK 17+ installation.
  exit /b 1
)

set WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar

if exist "%WRAPPER_JAR%" (
  "%JAVA_HOME%\bin\java" -jar "%WRAPPER_JAR%" %*
) else (
  mvn %*
)
