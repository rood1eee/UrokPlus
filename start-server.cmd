@echo off
setlocal

set "PROJECT_ROOT=C:\Users\igord\AndroidStudioProjects\UrokPlus"
set "SERVER_DIR=%PROJECT_ROOT%\server"
set "NODE_EXE=C:\Program Files\nodejs\node.exe"

cd /d "%SERVER_DIR%"

REM 1) Поднимаем сервер, если 8080 ещё не слушается
netstat -ano | findstr /R /C:":8080 .*LISTENING" >nul
if errorlevel 1 (
    start "UrokPlus Server" /min "%NODE_EXE%" index.js
    timeout /t 2 /nobreak >nul
)

REM 2) Поднимаем tuna, если ещё не запущен
tasklist /FI "IMAGENAME eq tuna.exe" | find /I "tuna.exe" >nul
if errorlevel 1 (
    start "UrokPlus Tuna" /min tuna http 8080
)

endlocal
