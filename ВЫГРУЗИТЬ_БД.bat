@echo off
chcp 65001 > nul
title UrokPlus Database Export
echo ======================================================
echo          UrokPlus: ВЫГРУЗКА БАЗЫ ДАННЫХ
echo ======================================================
echo.

:: Настройки
set DB_NAME=UrokPlus
set DB_USER=postgres
set OUTPUT_FILE=UrokPlus_backup_%date%.sql

echo [1/1] Выгрузка базы %DB_NAME% в файл %OUTPUT_FILE%...
echo Введите пароль от PostgreSQL (если потребуется)...

:: Запуск pg_dump
pg_dump -U %DB_USER% -d %DB_NAME% -f %OUTPUT_FILE%

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ------------------------------------------------------
    echo УСПЕХ! База успешно выгружена.
    echo Файл: %cd%\%OUTPUT_FILE%
    echo ------------------------------------------------------
) else (
    echo.
    echo [ОШИБКА] Не удалось выгрузить базу.
    echo Убедитесь, что PostgreSQL установлен и путь к bin добавлен в PATH.
)

echo.
pause