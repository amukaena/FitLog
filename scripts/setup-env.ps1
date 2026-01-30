# FitLog 개발 환경 설정 스크립트
# PowerShell 관리자 권한으로 실행하세요

Write-Host "=== FitLog 개발 환경 설정 ===" -ForegroundColor Cyan

# JAVA_HOME 설정
Write-Host "JAVA_HOME 설정 중..." -ForegroundColor Yellow
[Environment]::SetEnvironmentVariable("JAVA_HOME", "D:\00_JDK\jdk-17", "User")

# ANDROID_HOME 설정
Write-Host "ANDROID_HOME 설정 중..." -ForegroundColor Yellow
[Environment]::SetEnvironmentVariable("ANDROID_HOME", "D:\Android", "User")

# Path에 추가
Write-Host "Path 설정 중..." -ForegroundColor Yellow
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")

$pathsToAdd = @(
    "D:\00_JDK\jdk-17\bin",
    "D:\Android\cmdline-tools\latest\bin",
    "D:\Android\platform-tools"
)

foreach ($path in $pathsToAdd) {
    if ($currentPath -notlike "*$path*") {
        $currentPath = "$currentPath;$path"
        Write-Host "  추가됨: $path" -ForegroundColor Green
    } else {
        Write-Host "  이미 존재: $path" -ForegroundColor Gray
    }
}

[Environment]::SetEnvironmentVariable("Path", $currentPath, "User")

Write-Host ""
Write-Host "=== 설정 완료 ===" -ForegroundColor Cyan
Write-Host "VSCode와 터미널을 재시작하세요!" -ForegroundColor Yellow
Write-Host ""

# 현재 세션에도 적용
$env:JAVA_HOME = "D:\00_JDK\jdk-17"
$env:ANDROID_HOME = "D:\Android"
$env:Path = $currentPath

Write-Host "확인:" -ForegroundColor Cyan
Write-Host "  JAVA_HOME: $env:JAVA_HOME"
Write-Host "  ANDROID_HOME: $env:ANDROID_HOME"
