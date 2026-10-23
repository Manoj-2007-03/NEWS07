# NEWSROOM Desktop Reader Phase 2 PowerShell Launcher
$ErrorActionPreference = "Stop"

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host " NEWSROOM - Editorial Desktop Reader (Phase 2)" -ForegroundColor White
Write-Host "=================================================" -ForegroundColor Cyan

if (Test-Path "newsroom-2.0.0.jar") {
    Write-Host "Launching from packaged JAR..." -ForegroundColor Green
    java "-Dawt.useSystemAAFontSettings=on" "-Dswing.aatext=true" -jar newsroom-2.0.0.jar
} elseif (Test-Path "newsroom-1.0.0.jar") {
    Write-Host "Launching from packaged JAR..." -ForegroundColor Green
    java "-Dawt.useSystemAAFontSettings=on" "-Dswing.aatext=true" -jar newsroom-1.0.0.jar
} else {
    Write-Host "Launching from bin directory with SQLite classpath..." -ForegroundColor Green
    java "-Dawt.useSystemAAFontSettings=on" "-Dswing.aatext=true" -cp "bin;lib/*" com.newsroom.NewsReaderApplication
}
