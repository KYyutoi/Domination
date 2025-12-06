@echo off
REM Minecraft Server Launcher
REM このファイルをダブルクリックしてサーバーを起動

cd /d "%~dp0"
title Minecraft Server

cls
echo.
echo ========================================
echo   Minecraft Server Launcher
echo ========================================
echo.
echo Starting server...
echo.

REM サーバーを起動（エラーがあっても続行）
gradlew.bat runServer --console=plain 2>&1

REM 何があってもウィンドウを閉じない
echo.
echo.
echo ========================================
echo   Server stopped or error occurred
echo ========================================
echo.
echo Press any key to close...
pause >nul

