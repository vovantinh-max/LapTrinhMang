@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>&1
if errorlevel 1 (
    echo JDK 21 is required to build MiniHTTP.
    exit /b 1
)
if not exist "build\classes" mkdir "build\classes"
javac --release 21 -encoding UTF-8 -sourcepath "src" -d "build\classes" "src\MiniHttp.java"
if errorlevel 1 exit /b 1
jar --create --file "build\MiniHTTP.jar" --main-class MiniHttp -C "build\classes" .
exit /b %ERRORLEVEL%
