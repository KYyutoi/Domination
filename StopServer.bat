@echo off
title Stop Minecraft Server
echo Stopping Minecraft server...
cd /d "%~dp0"
gradlew --stop
echo.
echo Server stopped successfully!
timeout /t 3

