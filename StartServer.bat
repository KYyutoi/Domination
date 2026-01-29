@echo off
title Minecraft Server - Domination_test
color 0A
echo ========================================
echo   Minecraft Server - Domination_test
echo ========================================
echo.

REM プロジェクトディレクトリに移動
cd /d "%~dp0"
echo Current directory: %CD%
echo.

REM Gradlewが存在するか確認
if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat not found!
    echo Please make sure you are in the correct directory.
    echo.
    pause
    exit /b 1
)

echo [1/3] Stopping existing servers...
call gradlew.bat --stop
if errorlevel 1 (
    echo [WARNING] Could not stop existing servers.
    echo.
)
timeout /t 2 /nobreak >nul

echo.
echo [2/3] Cleaning lock files...
if exist "run\world\session.lock" del /f /q "run\world\session.lock" 2>nul
if exist "run\world_nether\session.lock" del /f /q "run\world_nether\session.lock" 2>nul
if exist "run\world_the_end\session.lock" del /f /q "run\world_the_end\session.lock" 2>nul
echo Lock files cleaned.

echo.
echo [3/3] Starting server...
echo This may take a few moments...
echo.
echo ========================================
echo   Server will start below
echo   Press Ctrl+C to stop the server
echo ========================================
echo.

call gradlew.bat runServer --console=plain
if errorlevel 1 (
    echo.
    echo ========================================
    echo [ERROR] Server failed to start!
    echo ========================================
    echo.
    pause
    exit /b 1
)

echo.
echo Server stopped.
pause

