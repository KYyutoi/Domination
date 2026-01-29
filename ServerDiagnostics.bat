@echo off
title Server Diagnostics
color 0E

echo ========================================
echo   Minecraft Server Diagnostics
echo ========================================
echo.

cd /d "%~dp0"

echo Current Directory:
echo %CD%
echo.

echo Checking for required files...
echo.

if exist "gradlew.bat" (
    echo [OK] gradlew.bat found
) else (
    echo [ERROR] gradlew.bat NOT found!
)

if exist "build.gradle" (
    echo [OK] build.gradle found
) else (
    echo [ERROR] build.gradle NOT found!
)

if exist "run\eula.txt" (
    echo [OK] run\eula.txt found
) else (
    echo [WARNING] run\eula.txt NOT found
)

echo.
echo Checking Java...
java -version 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found in PATH!
) else (
    echo [OK] Java is installed
)

echo.
echo Checking Gradle...
call gradlew.bat --version 2>&1 | findstr "Gradle"
if errorlevel 1 (
    echo [ERROR] Gradle not working properly
) else (
    echo [OK] Gradle is working
)

echo.
echo ========================================
echo   Diagnostics Complete
echo ========================================
echo.

echo Would you like to try starting the server? (Y/N)
set /p choice="> "

if /i "%choice%"=="Y" (
    echo.
    echo Starting server...
    call gradlew.bat runServer --console=plain
)

echo.
pause

