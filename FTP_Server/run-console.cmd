@echo off
setlocal
cd /d "%~dp0"
call build.cmd
if errorlevel 1 exit /b 1
echo Dang khoi chay FTP Server Console tren Port 2121...
java -cp "bin;lib/*" server.FtpServer 2121
pause
