# Minecraft Server Launcher - Domination_test
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Minecraft Server - Domination_test" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# プロジェクトディレクトリに移動
Set-Location $PSScriptRoot

# 既存のGradleデーモンを停止
Write-Host "Stopping existing Gradle daemons..." -ForegroundColor Yellow
& .\gradlew --stop | Out-Null
Start-Sleep -Seconds 2

# ロックファイルを削除
Write-Host "Cleaning up lock files..." -ForegroundColor Yellow
$lockFiles = @(
    ".\run\world\session.lock",
    ".\run\world_nether\session.lock",
    ".\run\world_the_end\session.lock"
)

foreach($file in $lockFiles) {
    if(Test-Path $file) {
        try {
            Remove-Item $file -Force
            Write-Host "  Removed: $file" -ForegroundColor Gray
        } catch {
            Write-Host "  Could not remove: $file (may not be locked)" -ForegroundColor DarkGray
        }
    }
}

Write-Host ""
Write-Host "Building and starting server..." -ForegroundColor Green
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Yellow
Write-Host ""

# サーバーを起動
& .\gradlew runServer --console=plain

Write-Host ""
Write-Host "Server stopped." -ForegroundColor Red
Read-Host "Press Enter to close this window"

