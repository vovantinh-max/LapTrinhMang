@echo off
setlocal
cd /d "%~dp0"
if not exist bin mkdir bin

powershell -NoProfile -ExecutionPolicy Bypass -Command "$files = Get-ChildItem -Path 'src', 'test' -Recurse -Filter '*.java' | ForEach-Object { Resolve-Path -Path $_.FullName -Relative }; Set-Content -Path 'sources.txt' -Value $files -Encoding ascii"

javac -encoding UTF-8 -cp "lib/*" -d "bin" "@sources.txt"
if errorlevel 1 (
    echo [ERROR] Bien dich that bai!
    exit /b 1
)

if exist sources.txt del sources.txt >nul 2>&1
echo [SUCCESS] Bien dich Mail Server & Client thanh cong 100%!
