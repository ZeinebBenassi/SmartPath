@echo off
echo ========================================
echo    Correction automatique Maven
echo ========================================

set PROJECT_DIR=%~dp0
set MAVEN_DIR=C:\maven-temp
set MAVEN_VERSION=3.9.6
set MAVEN_ZIP=%TEMP%\apache-maven.zip
set MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip

echo.
echo [1/4] Telechargement de Maven %MAVEN_VERSION%...
powershell -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%' -UseBasicParsing"
if errorlevel 1 (
    echo ERREUR: Echec du telechargement de Maven.
    pause
    exit /b 1
)
echo OK - Maven telecharge.

echo.
echo [2/4] Extraction de Maven...
if exist "%MAVEN_DIR%" rmdir /s /q "%MAVEN_DIR%"
powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%MAVEN_DIR%' -Force"
if errorlevel 1 (
    echo ERREUR: Echec de l'extraction.
    pause
    exit /b 1
)
echo OK - Maven extrait.

set MVN_EXE=%MAVEN_DIR%\apache-maven-%MAVEN_VERSION%\bin\mvn.cmd

echo.
echo [3/4] Resolution des dependances Maven...
cd /d "%PROJECT_DIR%"

"%MVN_EXE%" dependency:resolve -U
if errorlevel 1 (
    echo ERREUR: Echec de la resolution des dependances.
    pause
    exit /b 1
)
echo OK - Dependances resolues.

echo.
echo [4/4] Compilation du projet...
"%MVN_EXE%" clean compile -U
if errorlevel 1 (
    echo ERREUR: Echec de la compilation.
    pause
    exit /b 1
)

echo.
echo ========================================
echo    SUCCES ! Projet compile avec succes.
echo ========================================
echo.
echo Vous pouvez maintenant lancer le projet depuis IntelliJ.
pause
