# KIT AI Resume Screener — Local Run Script
# Usage: .\run.ps1
# Set your secrets in the .env file (never commit that file)

# Load from .env file if it exists
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match "^\s*([^#][^=]+)=(.*)$") {
            [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), "Process")
        }
    }
    Write-Host ".env loaded." -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "Starting KIT AI Resume Screener..." -ForegroundColor Cyan
Write-Host "Open http://localhost:8081 in your browser" -ForegroundColor Green
Write-Host "Login: admin / admin123" -ForegroundColor Yellow
Write-Host ""

& "d:\Resume-Screening-System\maven\bin\mvn.cmd" spring-boot:run
