@echo off
cd /d "%~dp0"
echo Starting UrokPlus API + Tuna tunnel...
echo.
start "UrokPlus API" cmd /k "cd /d ""%~dp0server"" && node index.js"
ping 127.0.0.1 -n 3 >nul
start "UrokPlus Tuna" powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-tuna-tunnel.ps1"
echo API window and Tuna window started.
echo.
pause
