@echo off
REM Backend Auto Setup & Launch
REM ============================

setlocal enabledelayedexpansion

set JAVA_HOME=C:\Program Files\Java\jdk1.8.0
set M2_HOME=C:\tools\maven
set PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%

echo.
echo ====================================
echo  Backend Setup ^& Launch
echo ====================================
echo.

REM Check JDK
if exist "%JAVA_HOME%\bin\javac.exe" (
    echo JDK found at %JAVA_HOME%
) else (
    echo JDK not found. Downloading...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$url = 'https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u382-b05/OpenJDK8U-jdk_x64_windows_hotspot_8u382b05.zip'; " ^
        "$zip = '$env:TEMP\jdk8.zip'; " ^
        "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; " ^
        "$client = New-Object System.Net.WebClient; " ^
        "$client.DownloadFile($url, $zip); " ^
        "Expand-Archive -Path $zip -DestinationPath 'C:\Program Files\Java' -Force; " ^
        "Get-ChildItem 'C:\Program Files\Java' -Directory | Where-Object {$_.Name -like 'jdk*'} | Select-Object -First 1 | Rename-Item -NewName 'jdk1.8.0' -Force; " ^
        "Remove-Item $zip -Force"
    
    if not exist "%JAVA_HOME%\bin\javac.exe" (
        echo ERROR: JDK installation failed
        echo Please install JDK 8 manually from: https://adoptium.net/
        pause
        exit /b 1
    )
    echo JDK installed successfully!
)

echo.
echo ====================================
echo  Verifying Setup
echo ====================================
echo.

java -version
echo.
mvn --version

echo.
echo ====================================
echo  Building...
echo ====================================
echo.

cd /d "%~dp0"
mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Build failed
    pause
    exit /b 1
)

echo.
echo ====================================
echo  SUCCESS! Starting Backend
echo  Backend running on: http://localhost:8080
echo ====================================
echo.

mvn spring-boot:run

pause
