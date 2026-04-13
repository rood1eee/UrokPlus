@echo off
chcp 65001 > nul
title UrokPlus Server Manager
echo ======================================================
echo          UrokPlus: ЗАПУСК СЕРВЕРА И ТУННЕЛЯ
echo ======================================================
echo.

:: Проверка наличия папки server
if not exist "server" (
    echo [ОШИБКА] Папка "server" не найдена!
    echo Убедитесь, что запускаете батник из корня проекта:
    echo C:\Users\igord\AndroidStudioProjects\UrokPlus
    pause
    exit
)

echo [1/2] Запуск Node.js сервера (порт 8080)...
start "UrokPlus API" cmd /k "cd server && npm start"

echo [2/2] Запуск туннеля Tuna (порт 8080)...
start "Tuna Tunnel" cmd /k "tuna http 8080"

echo.
echo ------------------------------------------------------
echo ВСЁ ГОТОВО!
echo 1. Сервер и Tuna открылись в отдельных окнах.
echo 2. Проверьте адрес в окне Tuna (Forwarding).
echo 3. Если адрес изменился, обновите его в build.gradle.kts
echo ------------------------------------------------------
echo.
pause