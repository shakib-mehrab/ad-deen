# Builds a signed release APK and the matching update manifest (update/version.json).
#
#   pwsh -File scripts/release.ps1 -Notes "What's new" [-NotesBn "নতুন কী"] [-MinSupported 0]
#
# Before running: raise versionCode/versionName in app/build.gradle.kts and create keystore.properties
# (see docs/RELEASE.md). After running, follow the printed steps (GitHub release, then push version.json).
param(
    [Parameter(Mandatory = $true)][string]$Notes,
    [string]$NotesBn = "",
    [int]$MinSupported = 0,
    [string]$Repo = "shakib-mehrab/ad-deen"
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Test-Path "keystore.properties")) { throw "keystore.properties is missing - see docs/RELEASE.md step 1." }

$gradle = Get-Content "app/build.gradle.kts" -Raw
$versionCode = [int]([regex]::Match($gradle, 'versionCode\s*=\s*(\d+)').Groups[1].Value)
$versionName = [regex]::Match($gradle, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
Write-Host "Releasing Ad-Deen $versionName (versionCode $versionCode)" -ForegroundColor Cyan

# Refuse to publish a versionCode that is not higher than the one already live.
if (Test-Path "update/version.json") {
    $live = Get-Content "update/version.json" -Raw | ConvertFrom-Json
    if ($live.versionCode -ge $versionCode -and $live.versionName -ne $versionName) {
        throw "versionCode $versionCode is not higher than the published $($live.versionCode). Bump it in app/build.gradle.kts."
    }
}

& .\gradlew.bat clean testDebugUnitTest assembleRelease --console=plain
if ($LASTEXITCODE -ne 0) { throw "Build or tests failed." }

$apk = "app/build/outputs/apk/release/app-release.apk"
$apksigner = Get-ChildItem "$env:ANDROID_HOME\build-tools" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
if (-not $apksigner) { $apksigner = Get-ChildItem "C:\Android\sdk\build-tools" -Directory | Sort-Object Name -Descending | Select-Object -First 1 }
& "$($apksigner.FullName)\apksigner.bat" verify $apk
if ($LASTEXITCODE -ne 0) { throw "APK signature verification failed." }

New-Item -ItemType Directory -Force dist | Out-Null
$fileName = "Ad-Deen-$versionName.apk"
Copy-Item $apk "dist/$fileName" -Force
# Stable-name copy: the website's download button always points at releases/latest/download/Ad-Deen.apk
Copy-Item $apk "dist/Ad-Deen.apk" -Force
Copy-Item "app/build/outputs/mapping/release/mapping.txt" "dist/mapping-$versionName.txt" -Force
$sha = (Get-FileHash "dist/$fileName" -Algorithm SHA256).Hash.ToLower()

$manifest = [ordered]@{
    versionCode = $versionCode
    versionName = $versionName
    apkUrl = "https://github.com/$Repo/releases/download/v$versionName/$fileName"
    sha256 = $sha
    notes = $Notes
    notesBn = $(if ($NotesBn) { $NotesBn } else { $Notes })
    minSupportedVersionCode = $MinSupported
}
New-Item -ItemType Directory -Force update | Out-Null
$manifest | ConvertTo-Json | Set-Content -Path "update/version.json" -Encoding utf8

Write-Host ""
Write-Host "Built dist/$fileName  (sha256 $sha)" -ForegroundColor Green
Write-Host "Saved dist/mapping-$versionName.txt (keep it to decode crash traces)"
Write-Host ""
Write-Host "Next steps (in this order):" -ForegroundColor Yellow
Write-Host "  1. GitHub -> Releases -> Draft a new release -> tag v$versionName -> attach BOTH dist/$fileName and dist/Ad-Deen.apk -> Publish"
Write-Host "     https://github.com/$Repo/releases/new?tag=v$versionName"
Write-Host "  2. Then publish the update manifest:"
Write-Host "     git add update/version.json app/build.gradle.kts; git commit -m `"release: v$versionName`"; git push"
Write-Host "  Users get the update prompt within 12 hours (or via More > Check for updates)."
