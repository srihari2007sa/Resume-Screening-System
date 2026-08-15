# KIT AI Resume Screener — Local Run Script
# Usage: 
#   .\run.ps1           -> Run with H2 database (default)
#   .\run.ps1 -mysql    -> Run with MySQL database
#   .\run.ps1 -h2       -> Run with H2 database (explicit)

param(
    [switch]$mysql,
    [switch]$h2,
    [switch]$help
)

if ($help) {
    Write-Host ""
    Write-Host "KIT AI Resume Screener - Run Options:" -ForegroundColor Cyan
    Write-Host "  .\run.ps1         -> H2 database (default)" -ForegroundColor Green
    Write-Host "  .\run.ps1 -mysql  -> MySQL database" -ForegroundColor Green  
    Write-Host "  .\run.ps1 -h2     -> H2 database (explicit)" -ForegroundColor Green
    Write-Host ""
    Write-Host "Database Profiles:" -ForegroundColor Yellow
    Write-Host "  H2:    Fast startup, file-based, good for development" 
    Write-Host "  MySQL: Production-like, requires MySQL server running"
    Write-Host ""
    return
}

# Determine profile
$profile = "default"  # H2 by default
if ($mysql) {
    $profile = "mysql"
    Write-Host "🗄️  Using MySQL Database Profile" -ForegroundColor Yellow
} elseif ($h2) {
    $profile = "default"
    Write-Host "🗄️  Using H2 Database Profile" -ForegroundColor Yellow
} else {
    Write-Host "🗄️  Using H2 Database Profile (default)" -ForegroundColor DarkGray
}

# Load from .env file if it exists
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match "^\s*([^#][^=]+)=(.*)$") {
            [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), "Process")
        }
    }
    Write-Host "📄 .env loaded." -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "🚀 Starting KIT AI Resume Screener..." -ForegroundColor Cyan
Write-Host "🌐 Open http://localhost:8081 in your browser" -ForegroundColor Green

if ($profile -eq "mysql") {
    Write-Host "🔑 Make sure MySQL is running with 'resume_screening_db' database" -ForegroundColor Yellow
}

Write-Host ""

# Run with selected profile
if ($profile -eq "mysql") {
    & "d:\Resume-Screening-System\maven\bin\mvn.cmd" spring-boot:run -D"spring-boot.run.profiles=mysql"
} else {
    & "d:\Resume-Screening-System\maven\bin\mvn.cmd" spring-boot:run
}
