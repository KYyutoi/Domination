# デスクトップにショートカットを作成するスクリプト
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Desktop Shortcut Creator" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$desktop = [Environment]::GetFolderPath("Desktop")
$projectPath = $PSScriptRoot

Write-Host "Desktop: $desktop" -ForegroundColor Gray
Write-Host "Project: $projectPath" -ForegroundColor Gray
Write-Host ""

# WScriptShellオブジェクトを作成
$WshShell = New-Object -ComObject WScript.Shell

try {
    # サーバー起動ショートカット
    $shortcutPath = Join-Path $desktop "Start Minecraft Server.lnk"
    $shortcut = $WshShell.CreateShortcut($shortcutPath)
    $shortcut.TargetPath = Join-Path $projectPath "StartServer.bat"
    $shortcut.WorkingDirectory = $projectPath
    $shortcut.Description = "Start Domination_test Minecraft Server"
    $shortcut.Save()
    Write-Host "[OK] Created: Start Minecraft Server.lnk" -ForegroundColor Green

    # サーバーランチャーショートカット
    $shortcutPath2 = Join-Path $desktop "Minecraft Server Launcher.lnk"
    $shortcut2 = $WshShell.CreateShortcut($shortcutPath2)
    $shortcut2.TargetPath = Join-Path $projectPath "ServerLauncher.bat"
    $shortcut2.WorkingDirectory = $projectPath
    $shortcut2.Description = "Minecraft Server Launcher with Menu"
    $shortcut2.Save()
    Write-Host "[OK] Created: Minecraft Server Launcher.lnk" -ForegroundColor Green

    Write-Host ""
    Write-Host "Shortcuts created successfully on your desktop!" -ForegroundColor Cyan
} catch {
    Write-Host ""
    Write-Host "[ERROR] Failed to create shortcuts: $_" -ForegroundColor Red
}

Write-Host ""
Read-Host "Press Enter to close"

