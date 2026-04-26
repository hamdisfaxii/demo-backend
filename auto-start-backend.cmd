@echo off
REM ============================================
REM  Gestion des Conges - Backend Auto Setup
REM ============================================

setlocal enabledelayedexpansion

set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0"
set "M2_HOME=C:\tools\maven"
set "JDK_URL=https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u382-b05/OpenJDK8U-jdk_x64_windows_hotspot_8u382b05.zip"
set "JDK_ZIP=%TEMP%\jdk8.zip"

echo.
echo ============================================
echo  Backend Setup & Launch
echo ============================================
echo.

REM Check if JDK exists
if exist "%JAVA_HOME%" (
    echo JDK already installed
) else (
    echo Installing JDK 8...
    echo Downloading from: %JDK_URL%
    
    powershell -Command "$ProgressPreference = 'SilentlyContinue'; [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%JDK_URL%' -OutFile '%JDK_ZIP%' -UseBasicParsing"
    
    if not exist "%JDK_ZIP%" (
        echo.
        echo ERROR: JDK download failed
        echo Try downloading manually from: https://adoptium.net/
        pause
        exit /b 1
    )
    
    echo Extracting JDK...
    powershell -Command "Expand-Archive -Path '%JDK_ZIP%' -DestinationPath 'C:\Program Files\Java' -Force"
    
    REM Find the extracted folder
    for /d %%D in (C:\Program Files\Java\jdk*) do (
        ren "%%D" jdk1.8.0
        goto :found
    )
    
    :found
    del "%JDK_ZIP%"
    echo JDK installed successfully!
)

echo.
echo ============================================
echo  Verifying Tools
echo ============================================
echo.

set "PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%"

java -version
mvn --version

echo.
echo ============================================
echo  Building Backend
echo ============================================
echo.

cd /d "%~dp0"

REM Clean build
mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo.
echo ============================================
echo  BUILD SUCCESS!
echo  Starting Backend...
echo ============================================
echo.

REM Run the application
mvn spring-boot:run

pause
