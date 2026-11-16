@echo off
setlocal
cd /d "%~dp0"
call build.cmd
if errorlevel 1 exit /b %errorlevel%
echo Dang chay bo kiem thu tu dong (MailIntegrationTest)...
java -ea "-Dlab.db.path=build/test-%RANDOM%.db" -cp "bin;lib/*" test.MailIntegrationTest
pause
