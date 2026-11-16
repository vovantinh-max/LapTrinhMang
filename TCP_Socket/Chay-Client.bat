@echo off
setlocal
title TCP Chat - Khoi dong Client
if not exist "%~dp0ChatRoom-TCP-Socket\dev.ps1" (
    echo Khong tim thay thu muc ChatRoom-TCP-Socket ben canh file nay.
    pause
    exit /b 1
)
pushd "%~dp0ChatRoom-TCP-Socket"
where javaw.exe >nul 2>&1
if errorlevel 1 goto failed
echo Dang bien dich va khoi dong Client...
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ".\dev.ps1" build
if errorlevel 1 goto failed
start "" javaw.exe -cp "build/classes;lib/*" client.ui.LoginFrame
if errorlevel 1 goto failed
popd
exit /b 0

:failed
echo.
echo Khong the khoi dong Client. Hay kiem tra thong bao loi o tren.
echo Can cai JDK 23 va them Java vao PATH.
popd
pause
exit /b 1
