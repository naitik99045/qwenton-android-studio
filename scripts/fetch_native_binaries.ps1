# ==============================================================================
# Qwenton Studio Apex — Native ARM64 Binaries Fetcher (PowerShell)
# Fetches pre-compiled ARM64 binaries into app/src/main/jniLibs/arm64-v8a/
# ==============================================================================

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$DestDir = Join-Path (Split-Path -Parent $ScriptDir) "app\src\main\jniLibs\arm64-v8a"

if (-not (Test-Path $DestDir)) {
    New-Item -ItemType Directory -Force -Path $DestDir | Out-Null
}

Write-Host "==> Target ARM64 jniLibs directory: $DestDir" -ForegroundColor Cyan

$CloudflaredUrl = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64"
$ProotUrl = "https://github.com/termux/proot/releases/download/v5.1.107/proot-v5.1.107-aarch64"

Write-Host "==> [1/3] Downloading Cloudflare Quick Tunnel binary (libcloudflared.so)..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri $CloudflaredUrl -OutFile (Join-Path $DestDir "libcloudflared.so") -UseBasicParsing
    Write-Host " [OK] libcloudflared.so saved." -ForegroundColor Green
} catch {
    Write-Warning "Could not download cloudflared automatically: $_"
}

Write-Host "==> [2/3] Downloading PRoot binary (libproot.so)..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri $ProotUrl -OutFile (Join-Path $DestDir "libproot.so") -UseBasicParsing
    Write-Host " [OK] libproot.so saved." -ForegroundColor Green
} catch {
    Write-Warning "Could not download proot automatically: $_"
}

Write-Host "==> [3/3] Note: libllama_server.so can be compiled from llama.cpp Android NDK or fetched from releases." -ForegroundColor Yellow
Write-Host "==> Directory status:" -ForegroundColor Cyan
Get-ChildItem -Path $DestDir
