#!/usr/bin/env pwsh

# ============================================
# Backend Auto-Setup & Launch Script
# ============================================

$ErrorActionPreference = "Stop"

$JAVA_HOME = "C:\Program Files\Java\jdk1.8.0"
$M2_HOME = "C:\tools\maven"
$JDK_ZIP = "$env:TEMP\jdk8.zip"
$JDK_DEST = "C:\Program Files\Java"

# JDK URLs (try multiple mirrors)
$JDK_URLS = @(
    "https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u382-b05/OpenJDK8U-jdk_x64_windows_hotspot_8u382b05.zip",
    "https://api.azul.com/zulu/download/community/v15.0.11.1/zulu15.40.17-ca-jdk15.0.11_windows_x64.zip"
)

function Download-File {
    param(
        [string]$Url,
        [string]$OutFile
    )
    
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        $web = New-Object System.Net.WebClient
        $web.DownloadFile($Url, $OutFile)
        return $true
    } catch {
        return $false
    }
}

Write-Host "`n============================================" -ForegroundColor Green
Write-Host "  Gestion des Conges - Backend Setup" -ForegroundColor Green
Write-Host "============================================`n" -ForegroundColor Green

# Check/Install JDK
if (Test-Path "$JAVA_HOME\bin\javac.exe") {
    Write-Host "✓ JDK already installed" -ForegroundColor Green
} else {
    Write-Host "Installing JDK 8..." -ForegroundColor Yellow
    
    $downloaded = $false
    foreach ($url in $JDK_URLS) {
        Write-Host "Trying: $url" -ForegroundColor Gray
        if (Download-File -Url $url -OutFile $JDK_ZIP) {
            Write-Host "Downloaded!" -ForegroundColor Green
            $downloaded = $true
            break
        }
    }
    
    if (-not $downloaded) {
        Write-Host "ERROR: Could not download JDK" -ForegroundColor Red
        Write-Host "Download manually from: https://adoptium.net/" -ForegroundColor Yellow
        exit 1
    }
    
    Write-Host "Extracting JDK..." -ForegroundColor Yellow
    Expand-Archive -Path $JDK_ZIP -DestinationPath $JDK_DEST -Force
    
    # Find and rename the extracted folder
    $jdkFolder = Get-ChildItem $JDK_DEST -Directory | Where-Object { $_.Name -like "jdk*" -or $_.Name -like "*openj*" } | Select-Object -First 1
    if ($jdkFolder) {
        Rename-Item $jdkFolder.FullName "$JAVA_HOME" -Force
        Write-Host "✓ JDK installed!" -ForegroundColor Green
    }
    
    Remove-Item $JDK_ZIP -Force -ErrorAction SilentlyContinue
}

Write-Host "`n============================================" -ForegroundColor Green
Write-Host "  Checking Tools" -ForegroundColor Green
Write-Host "============================================`n" -ForegroundColor Green

$env:JAVA_HOME = $JAVA_HOME
$env:M2_HOME = $M2_HOME
$env:Path = "$JAVA_HOME\bin;$M2_HOME\bin;$env:Path"

java -version
Write-Host ""
mvn --version

Write-Host "`n============================================" -ForegroundColor Green
Write-Host "  Building Backend" -ForegroundColor Green
Write-Host "============================================`n" -ForegroundColor Green

Set-Location (Split-Path $MyInvocation.MyCommand.Path)

# Build
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "`n============================================" -ForegroundColor Green
Write-Host "  BUILD SUCCESS!" -ForegroundColor Green
Write-Host "  Starting Backend on http://localhost:8080" -ForegroundColor Green
Write-Host "============================================`n" -ForegroundColor Green

# Run
mvn spring-boot:run
