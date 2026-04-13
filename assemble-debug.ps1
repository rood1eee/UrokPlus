# Сборка debug APK, если R.jar «занят» (Cursor / Java LS). Запуск из корня проекта.
$ErrorActionPreference = 'SilentlyContinue'
Set-Location $PSScriptRoot
& .\gradlew.bat --stop
Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
    Where-Object {
        $_.CommandLine -match 'gradle-server|redhat\.java|vscode-gradle|fwcd\.kotlin|jdt\.ls|org\.eclipse\.jdt'
    } |
    ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
Start-Sleep -Seconds 2
Remove-Item -Recurse -Force ".\app\build" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force ".\.gradle-android-build" -ErrorAction SilentlyContinue
$ErrorActionPreference = 'Stop'
& .\gradlew.bat assembleDebug @args
