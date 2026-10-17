$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$apk = "$projectRoot\app\build\outputs\apk\debug\app-debug.apk"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$pcDownloads = "$env:USERPROFILE\Downloads\KTSAdvancedMediaPlayer.apk"

# Build debug APK if not present or needs build
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
& "$projectRoot\gradlew.bat" assembleDebug

if (Test-Path $apk) {
    # 1. Copy to PC Downloads
    Copy-Item -Path $apk -Destination $pcDownloads -Force
    Write-Host "[+] Copied APK to PC Downloads: $pcDownloads" -ForegroundColor Green

    # 2. Check ADB
    if (Test-Path $adb) {
        $devices = & $adb devices
        if ($devices -match "device\b") {
            # Install to phone
            Write-Host "[*] Installing APK to connected phone..." -ForegroundColor Cyan
            & $adb install -r -d $apk

            # Save to phone Downloads
            Write-Host "[*] Saving APK to phone Download folder (/sdcard/Download/)..." -ForegroundColor Cyan
            & $adb push $apk "/sdcard/Download/KTSAdvancedMediaPlayer.apk"
            Write-Host "[+] Successfully installed on phone and saved to /sdcard/Download/KTSAdvancedMediaPlayer.apk" -ForegroundColor Green
        } else {
            Write-Host "[!] No Android device connected via ADB." -ForegroundColor Yellow
        }
    } else {
        Write-Host "[!] ADB not found at $adb" -ForegroundColor Yellow
    }
} else {
    Write-Host "[!] APK build failed or APK not found at $apk" -ForegroundColor Red
}
