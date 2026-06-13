@echo off
rem jpackage-build.cmd — reference command for the GABI app-image build.
rem Run from the repository root after: mvn -q -DskipTests package
rem
rem The --main-class is the Spring Boot 4.x JarLauncher (org.springframework.boot.loader.launch).
rem --win-console keeps the console window open so the GABI CLI is usable.
rem See packaging\README-packaging.md for full documentation.

jpackage ^
    --type app-image ^
    --name GABI ^
    --app-version 1.0.0 ^
    --input target ^
    --main-jar gabi-1.0.0-exec.jar ^
    --main-class org.springframework.boot.loader.launch.JarLauncher ^
    --dest packaging\bin ^
    --win-console
