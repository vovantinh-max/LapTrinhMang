@echo off
setlocal
cd /d "%~dp0"
call build.cmd
if errorlevel 1 exit /b 1
echo Dang khoi chay Mail Server Dashboard...
start javaw -cp "bin;lib/*" Main
