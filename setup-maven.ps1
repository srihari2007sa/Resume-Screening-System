# Setup local, portable Apache Maven
$mavenVersion = "3.9.6"
$downloadUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
$outputZip = Join-Path $PSScriptRoot "maven.zip"
$extractPath = Join-Path $PSScriptRoot "maven-temp"
$finalPath = Join-Path $PSScriptRoot "maven"

if (Test-Path $finalPath) {
    Write-Host "Maven is already configured at $finalPath" -ForegroundColor Green
    exit 0
}

Write-Host "Downloading Apache Maven $mavenVersion..." -ForegroundColor Cyan
try {
    Invoke-WebRequest -Uri $downloadUrl -OutFile $outputZip -UserAgent "Mozilla/5.0"
} catch {
    Write-Host "Download failed: $_" -ForegroundColor Red
    exit 1
}

Write-Host "Extracting Maven..." -ForegroundColor Cyan
try {
    if (Test-Path $extractPath) { Remove-Item -Path $extractPath -Recurse -Force }
    New-Item -ItemType Directory -Path $extractPath -Force | Out-Null
    Expand-Archive -Path $outputZip -DestinationPath $extractPath -Force
    
    $extractedFolder = Get-ChildItem -Path $extractPath | Where-Object { $_.PSIsContainer } | Select-Object -First 1
    if ($extractedFolder) {
        Move-Item -Path $extractedFolder.FullName -Destination $finalPath -Force
        Write-Host "Maven configured successfully at $finalPath!" -ForegroundColor Green
    } else {
        Write-Host "Could not find extracted maven folder." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Extraction failed: $_" -ForegroundColor Red
    exit 1
} finally {
    if (Test-Path $outputZip) { Remove-Item -Path $outputZip -Force }
    if (Test-Path $extractPath) { Remove-Item -Path $extractPath -Recurse -Force }
}
