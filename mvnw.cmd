@REM Maven wrapper script (Windows)
@REM SPEC-R01: launches the pinned Maven via the wrapper jar on the CLASSPATH with the
@REM explicit main class. The wrapper jar (maven-wrapper-3.2.0.jar) has NO Main-Class in
@REM its manifest, so `java -jar` fails ("no main manifest attribute"); it must be run as
@REM `java -classpath <jar> org.apache.maven.wrapper.MavenWrapperMain`.

@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

if "%JAVA_HOME%"=="" (
  set JAVACMD=java
) else (
  set JAVACMD=%JAVA_HOME%\bin\java
)

if exist "%WRAPPER_JAR%" (
  "%JAVACMD%" -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %*
) else (
  mvn %*
)
