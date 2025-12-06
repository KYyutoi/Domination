@echo off
title Minecraft Server Launcher
color 0A

:MENU
cls
echo ========================================
echo   MINECRAFT SERVER LAUNCHER
echo   Domination_test v1.0
echo ========================================
echo.
echo [1] Start Server
echo [2] Stop Server
echo [3] Build Plugin Only
echo [4] Clean Build
echo [5] View Logs
echo [6] Exit
echo.
set /p choice="Select option (1-6): "

if "%choice%"=="1" goto START
if "%choice%"=="2" goto STOP
if "%choice%"=="3" goto BUILD
if "%choice%"=="4" goto CLEAN
if "%choice%"=="5" goto LOGS
if "%choice%"=="6" goto EXIT

echo Invalid option!
timeout /t 2 >nul
goto MENU

:START
cls
echo Starting server...
cd /d "%~dp0"
gradlew --stop
gradlew runServer --console=plain
pause
goto MENU

:STOP
cls
echo Stopping server...
cd /d "%~dp0"
gradlew --stop
echo Server stopped!
timeout /t 3
goto MENU

:BUILD
cls
echo Building plugin...
cd /d "%~dp0"
gradlew build
echo.
echo Build complete!
pause
goto MENU

:CLEAN
cls
echo Cleaning and rebuilding...
cd /d "%~dp0"
gradlew clean build
echo.
echo Clean build complete!
pause
goto MENU

:LOGS
cls
if exist "run\logs\latest.log" (
    type "run\logs\latest.log"
) else (
    echo No log file found!
)
echo.
pause
goto MENU

:EXIT
echo Goodbye!
timeout /t 1 >nul
exit

