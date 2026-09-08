# Release system APK: bump build (versionCode), assemble, platform-sign, name output.
#
# Usage:
#   .\scripts\release-system-apk.ps1           # VERSION_CODE += 1, then build
#   .\scripts\release-system-apk.ps1 -NoBump  # build current version without bump
#   .\scripts\release-system-apk.ps1 -VersionName 0.0.4   # set marketing version, then bump code
#
# Version lives in app\version.properties (VERSION_NAME + VERSION_CODE).
# Output: install\out\geely-ex2-tools-v{VERSION_NAME}({VERSION_CODE}).apk
# Also copies to install\out\geely-ex2-tools-system-platform-signed.apk (stable path for docs).

param(
    [switch]$NoBump,
    [string]$VersionName = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

$versionFile = Join-Path $root "app\version.properties"
if (-not (Test-Path $versionFile)) {
    @"
VERSION_NAME=0.0.1
VERSION_CODE=0
"@ | Set-Content -Path $versionFile -Encoding ASCII
}

function Read-VersionProps([string]$path) {
    $map = @{}
    Get-Content $path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) { return }
        $parts = $line -split "=", 2
        if ($parts.Count -eq 2) {
            $map[$parts[0].Trim()] = $parts[1].Trim()
        }
    }
    return $map
}

function Write-VersionProps([string]$path, [hashtable]$map) {
    $lines = @(
        "VERSION_NAME=$($map['VERSION_NAME'])",
        "VERSION_CODE=$($map['VERSION_CODE'])"
    )
    Set-Content -Path $path -Value $lines -Encoding ASCII
}

$props = Read-VersionProps $versionFile
if (-not $props.ContainsKey("VERSION_NAME")) { $props["VERSION_NAME"] = "0.0.1" }
if (-not $props.ContainsKey("VERSION_CODE")) { $props["VERSION_CODE"] = "0" }

if ($VersionName) {
    $props["VERSION_NAME"] = $VersionName
}

$code = [int]$props["VERSION_CODE"]
if (-not $NoBump) {
    $code += 1
    $props["VERSION_CODE"] = "$code"
    Write-VersionProps $versionFile $props
    Write-Host "==> bumped VERSION_CODE -> $code (VERSION_NAME=$($props['VERSION_NAME']))"
} else {
    Write-Host "==> no bump (VERSION_NAME=$($props['VERSION_NAME']) VERSION_CODE=$code)"
}

$name = $props["VERSION_NAME"]
$code = [int]$props["VERSION_CODE"]
$apkFileName = "geely-ex2-tools-v${name}($code).apk"

$keysDir = Join-Path $root "keys\aosp-platform"
$pk8 = Join-Path $keysDir "platform.pk8"
$pem = Join-Path $keysDir "platform.x509.pem"
if (-not (Test-Path $pk8) -or -not (Test-Path $pem)) {
    throw "Missing AOSP platform keys in keys\aosp-platform (platform.pk8 / platform.x509.pem)"
}

Write-Host "==> assembleSystemRelease ($name / $code)"
& .\gradlew.bat :app:assembleSystemRelease --quiet
if ($LASTEXITCODE -ne 0) { throw "Gradle assembleSystemRelease failed" }

$built = Get-ChildItem "app\build\outputs\apk\system\release\*.apk" |
    Where-Object { $_.Name -notlike "*-unsigned*" } |
    Select-Object -First 1
if (-not $built) {
    $built = Get-ChildItem "app\build\outputs\apk\system\release\*.apk" | Select-Object -First 1
}
if (-not $built) { throw "No system release APK found" }

$outDir = Join-Path $root "install\out"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = "$env:LOCALAPPDATA\Android\Sdk" }
$apksigner = Get-ChildItem "$sdk\build-tools" -Recurse -Filter "apksigner.bat" |
    Sort-Object FullName -Descending |
    Select-Object -First 1 -ExpandProperty FullName
$zipalign = Get-ChildItem "$sdk\build-tools" -Recurse -Filter "zipalign.exe" |
    Sort-Object FullName -Descending |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $apksigner) { throw "apksigner.bat not found under $sdk\build-tools" }

$aligned = Join-Path $outDir "_tmp-aligned.apk"
$tmpUnsigned = Join-Path $outDir "_tmp-stripped.apk"
$signedNamed = Join-Path $outDir $apkFileName
$signedStable = Join-Path $outDir "geely-ex2-tools-system-platform-signed.apk"

foreach ($f in @($aligned, $tmpUnsigned, $signedNamed, $signedStable)) {
    if (Test-Path $f) { Remove-Item $f -Force }
}

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
Copy-Item $built.FullName $tmpUnsigned -Force
$zip = [System.IO.Compression.ZipFile]::Open(
    $tmpUnsigned,
    [System.IO.Compression.ZipArchiveMode]::Update
)
$toRemove = @($zip.Entries | Where-Object { $_.FullName -like "META-INF/*" })
foreach ($e in $toRemove) { $e.Delete() }
$zip.Dispose()

if ($zipalign) {
    & $zipalign -f -p 4 $tmpUnsigned $aligned
    if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }
} else {
    Copy-Item $tmpUnsigned $aligned -Force
}

& $apksigner sign `
    --key $pk8 `
    --cert $pem `
    --v1-signing-enabled true `
    --v2-signing-enabled true `
    --out $signedNamed `
    $aligned
if ($LASTEXITCODE -ne 0) { throw "apksigner sign failed" }

Copy-Item $signedNamed $signedStable -Force
Remove-Item $aligned, $tmpUnsigned -Force -ErrorAction SilentlyContinue

$ErrorActionPreference = "Continue"
cmd /c "`"$apksigner`" verify --print-certs `"$signedNamed`"" | Select-String "Signer #1 certificate"
$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "OK: $signedNamed"
Write-Host "    versionName=$name  versionCode=$code  (Android shows $name ($code))"
Write-Host "Also: $signedStable"
Write-Host ""
Write-Host "Install:"
Write-Host "  adb uninstall com.geely.ex2.tools"
Write-Host "  adb push `"$signedNamed`" /data/local/tmp/geely-ex2-tools-system.apk"
Write-Host "  adb shell pm install -r -g /data/local/tmp/geely-ex2-tools-system.apk"
