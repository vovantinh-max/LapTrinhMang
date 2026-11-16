$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "[1/2] Preparing bin directory..." -ForegroundColor Cyan
if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Force "bin" | Out-Null
}

Write-Host "[2/2] Compiling Mail Suite sources..." -ForegroundColor Cyan
$files = Get-ChildItem -Path "src", "test" -Recurse -Filter "*.java" | ForEach-Object { Resolve-Path -Path $_.FullName -Relative }
Set-Content -Path "sources.txt" -Value $files -Encoding ascii

& javac -encoding UTF-8 -cp "lib/*" -d "bin" "@sources.txt"

if (Test-Path "sources.txt") {
    Remove-Item -Force "sources.txt"
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] Biên dịch Mail Server & Client thành công 100%!" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Biên dịch thất bại!" -ForegroundColor Red
}

if ($LASTEXITCODE -ne 0) { throw "Compilation failed ($LASTEXITCODE)" }
