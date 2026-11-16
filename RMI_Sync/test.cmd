@echo off
setlocal
cd /d "%~dp0"
call build.cmd
if errorlevel 1 exit /b %errorlevel%
echo =======================================================
echo Dang chay Automated Integration Test RMI Dual Server...
echo =======================================================
java -cp "bin;lib/*" test.RmiSyncIntegrationTest
pause
