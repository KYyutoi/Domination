@echo off
title Minecraft Server - Simple Start
color 0A

REM このバッチファイルのディレクトリに移動
cd /d "%~dp0"

echo ========================================
echo   Minecraft Server - Simple Launcher
echo ========================================
echo.
echo Starting server, please wait...
echo.

REM Gradlewを使ってサーバーを起動
gradlew.bat runServer --console=plain

REM エラーが発生した場合
if errorlevel 1 (
    echo.
    echo ========================================
    echo ERROR: Failed to start server
    echo ========================================
    echo.
)

echo.
echo Press any key to close this window...
pause >nul

