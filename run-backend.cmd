@echo off
REM Maven Setup and Build Script for Windows

setlocal enabledelayedexpansion

set MAVEN_HOME=C:\tools\maven
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_451
set DOWNLOAD_URL=https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip
set TEMP_ZIP=%TEMP%\maven.zip

echo ========================================
echo  Checking Maven installation...
echo ========================================

if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Maven is already installed at %MAVEN_HOME%
) else (
    echo Maven not found. Downloading from Apache Repository...
    echo This may take a few minutes...
    
    if not exist C:\tools (
        mkdir C:\tools
    )
    
    powershell -Command "$ProgressPreference = 'SilentlyContinue'; Import-Module BitsTransfer; Start-BitsTransfer -Source '%DOWNLOAD_URL%' -Destination '%TEMP_ZIP%'" 2>nul || powershell -Command "$ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%TEMP_ZIP%' -UseBasicParsing"
    
    if exist "%TEMP_ZIP%" (
        echo Extracting Maven...
        powershell -Command "Expand-Archive -Path '%TEMP_ZIP%' -DestinationPath 'C:\tools' -Force" 2>nul || tar -xf "%TEMP_ZIP%" -C "C:\tools"
        
        REM Rename the extracted folder
        if exist "C:\tools\apache-maven-3.9.6" (
            ren "C:\tools\apache-maven-3.9.6" maven
            echo Maven installed successfully!
        )
        del "%TEMP_ZIP%"
    ) else (
        echo Failed to download Maven. Please download manually from: https://maven.apache.org/download.cgi
        pause
        exit /b 1
    )
)

echo ========================================
echo  Building Spring Boot Backend...
echo ========================================

cd /d "%~dp0"

REM Set PATH
set PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%

REM Verify Java and Maven
java -version
mvn -version

REM Build
mvn clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo  BUILD SUCCESSFUL!
    echo  Starting Spring Boot Backend...
    echo ========================================
    echo.
    
    REM Run the application
    mvn spring-boot:run
) else (
    echo.
    echo BUILD FAILED!
    echo Check the error messages above.
    pause
    exit /b 1
)

endlocal
pause
