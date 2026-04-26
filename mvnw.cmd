@REM Maven Wrapper Batch Script
@REM ============================================================================
@REM Maven Wrapper Startup Batch
@REM ============================================================================

@if "%DEBUG%"=="" @echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@REM Remove trailing backslash to avoid escaping quotes in -D arg
if "%DIRNAME:~-1%"=="\" set DIRNAME=%DIRNAME:~0,-1%

@REM Download Maven wrapper
if not exist "%DIRNAME%\.mvn\wrapper\maven-wrapper.jar" (
    echo Downloading Maven wrapper...
    powershell -Command "$ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar' -OutFile '%DIRNAME%\.mvn\wrapper\maven-wrapper.jar'"
)

@REM Execute Maven
java -classpath "%DIRNAME%\.mvn\wrapper\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=%DIRNAME%" org.apache.maven.wrapper.MavenWrapperMain %*
