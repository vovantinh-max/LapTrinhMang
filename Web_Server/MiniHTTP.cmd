@echo off
setlocal
cd /d "%~dp0"
if not exist "build\MiniHTTP.jar" (
    call "%~dp0Build.cmd"
    if errorlevel 1 (
        pause
        exit /b 1
    )
)
where java >nul 2>&1
if errorlevel 1 (
    echo Java 21 is required to run MiniHTTP.
    pause
    exit /b 1
)
java -jar "build\MiniHTTP.jar"
if errorlevel 1 pause
