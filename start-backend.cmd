@echo off
REM Backend Setup and Run Script
REM ============================

echo.
echo ========================================
echo   Gestion des Conges - Backend Setup
echo ========================================
echo.

REM Set Environment
set JAVA_HOME=C:\Program Files\Java\jre1.8.0_451
set M2_HOME=C:\tools\maven
set MAVEN_HOME=C:\tools\maven
set PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%

echo Checking versions...
cmd /c "C:\tools\maven\bin\mvn --version"
echo.

REM Navigate to project
cd /d "%~dp0"

echo.
echo ========================================
echo   Building Backend with Maven...
echo ========================================
echo.

REM Build with Maven
cmd /c "C:\tools\maven\bin\mvn clean package -DskipTests"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   BUILD SUCCESSFUL!
    echo   Starting Spring Boot Backend...
    echo ========================================
    echo.
    
    REM Run the application
    cmd /c "C:\tools\maven\bin\mvn spring-boot:run"
) else (
    echo.
    echo ========================================
    echo   BUILD FAILED
    echo ========================================
    echo.
    echo The project could not be compiled.
    echo This is likely because only Java Runtime Environment (JRE) is installed.
    echo Java Development Kit (JDK) is required to compile Java code.
    echo.
    echo Solution: Install Java JDK 8 or higher from:
    echo   https://www.oracle.com/java/technologies/downloads/#java8
    echo.
    pause
    exit /b 1
)

pause
