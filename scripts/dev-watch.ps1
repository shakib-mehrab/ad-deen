# Dev loop: watches app sources, then rebuilds, reinstalls and relaunches Ad-Deen on the connected device.
# Usage (from repo root):  pwsh -File scripts/dev-watch.ps1
$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$component = 'com.aistudio.addin.mqzpvt/com.example.MainActivity'
$watchDir = Join-Path $root 'app\src\main'

function Deploy {
    Write-Host "`n[$(Get-Date -Format HH:mm:ss)] Building & installing..." -ForegroundColor Cyan
    Push-Location $root
    & .\gradlew.bat installDebug --console=plain -q
    $ok = $LASTEXITCODE -eq 0
    Pop-Location
    if ($ok) {
        adb shell am start -S -n $component | Out-Null
        Write-Host "[$(Get-Date -Format HH:mm:ss)] Launched. Watching for changes..." -ForegroundColor Green
    } else {
        Write-Host "[$(Get-Date -Format HH:mm:ss)] Build FAILED - fix the error above; still watching." -ForegroundColor Red
    }
}

$watcher = New-Object System.IO.FileSystemWatcher $watchDir
$watcher.IncludeSubdirectories = $true
$watcher.NotifyFilter = [System.IO.NotifyFilters]'LastWrite, FileName'
$watcher.EnableRaisingEvents = $true

$script:dirty = $false
$handler = { $script:dirty = $true }
$subs = @(
    Register-ObjectEvent $watcher Changed -Action $handler
    Register-ObjectEvent $watcher Created -Action $handler
    Register-ObjectEvent $watcher Deleted -Action $handler
    Register-ObjectEvent $watcher Renamed -Action $handler
)

Deploy
try {
    while ($true) {
        Start-Sleep -Milliseconds 500
        if ($script:dirty) {
            # Debounce: wait until saves settle for 1.5s
            do { $script:dirty = $false; Start-Sleep -Milliseconds 1500 } while ($script:dirty)
            Deploy
        }
    }
} finally {
    $subs | ForEach-Object { Unregister-Event -SourceIdentifier $_.Name }
    $watcher.Dispose()
}
