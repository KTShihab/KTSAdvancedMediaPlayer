param(
    [string]$deviceAddress = "192.168.1.43:36629"
)

# Deploy APK script for KTS Advanced Media Player
$ErrorActionPreference = "Stop"

$jdkPath = "C:\Program Files\Android\Android Studio\jbr"
$adbPath = "C:\Users\Shiha\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$projectRoot = $PSScriptRoot
$apkSource = "$projectRoot\app\build\outputs\apk\release\app-release.apk"
$pcDownload = "C:\Users\Shiha\Downloads\KTSAdvancedMediaPlayer.apk"

Write-Host ">>> Building Release (Optimized) APK..." -ForegroundColor Cyan
$env:JAVA_HOME = $jdkPath
& "$projectRoot\gradlew.bat" assembleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

if (-not (Test-Path $apkSource)) {
    Write-Error "APK not found at $apkSource"
    exit 1
}

Write-Host ">>> 1. Copying to PC Downloads folder ($pcDownload)..." -ForegroundColor Green
Copy-Item -Path $apkSource -Destination $pcDownload -Force
Write-Host "Copied successfully to PC Downloads!" -ForegroundColor Green

if (Test-Path $adbPath) {
    if ($deviceAddress) {
        Write-Host ">>> Connecting to wireless device $deviceAddress..." -ForegroundColor Cyan
        & $adbPath connect $deviceAddress | Out-Null
    }
    $rawDevices = & $adbPath devices
    $matchedId = $null
    foreach ($line in $rawDevices) {
        if ($line -match '^(\S+)\s+device$') {
            $matchedId = $matches[1]
            break
        }
    }
    if ($matchedId) {
        $deviceId = $matchedId
        Write-Host ">>> Connected Android device detected: $deviceId" -ForegroundColor Cyan
        
        Write-Host ">>> 2. Transferring APK to Phone Download folder (/sdcard/Download/)..." -ForegroundColor Green
        & $adbPath -s $deviceId push $apkSource "/sdcard/Download/KTSAdvancedMediaPlayer.apk"
        
        Write-Host ">>> 3. Installing APK on phone..." -ForegroundColor Green
        & $adbPath -s $deviceId install -r -d $apkSource
        Write-Host "Installed successfully on phone!" -ForegroundColor Green
    } else {
        Write-Host ">>> No connected phone found via ADB. Skipped phone install/transfer." -ForegroundColor Yellow
    }
} else {
    Write-Host ">>> ADB not found at $adbPath." -ForegroundColor Yellow
}
