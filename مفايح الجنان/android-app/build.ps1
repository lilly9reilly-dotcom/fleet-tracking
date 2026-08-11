# Rebuild Al Kawthar (الكوثر) APK
$ErrorActionPreference = "Stop"
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot"
$env:ANDROID_HOME = "C:\Users\Sniper-313\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:Path = "C:\Users\Sniper-313\AppData\Local\gradle\gradle-8.13\bin;$env:JAVA_HOME\bin;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:ANDROID_HOME\platform-tools;" + $env:Path

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location -LiteralPath $root
$parent = Split-Path $root -Parent

# Source of truth: parent index.html (full app). Sync into www with cordova.js only.
$parentHtml = Join-Path $parent "index.html"
$wwwHtml = Join-Path $root "www\index.html"
$html = [System.IO.File]::ReadAllText($parentHtml)
if ($html -notmatch 'cordova\.js') {
  $html = $html.Replace('<script>', "<script src=`"cordova.js`"></script>`n<script>")
}
[System.IO.File]::WriteAllText($wwwHtml, $html, $utf8NoBom)

# Sync media manifest if present at parent
$parentManifest = Join-Path $parent "js\media-manifest.js"
$wwwManifest = Join-Path $root "www\js\media-manifest.js"
if (Test-Path -LiteralPath $parentManifest) {
  New-Item -ItemType Directory -Force -Path (Split-Path $wwwManifest) | Out-Null
  Copy-Item -LiteralPath $parentManifest -Destination $wwwManifest -Force
}

# Ensure startup audio folder exists (user can drop salawat_open.mp3)
$startupDir = Join-Path $root "www\audio\startup"
New-Item -ItemType Directory -Force -Path $startupDir | Out-Null

npx cordova prepare android | Out-Host

$props = Join-Path $root "platforms\android\gradle.properties"
$p = [System.IO.File]::ReadAllText($props)
if ($p -notmatch 'android\.overridePathCheck') {
  [System.IO.File]::AppendAllText($props, "`nandroid.overridePathCheck=true`n", $utf8NoBom)
}

$manifest = Join-Path $root "platforms\android\app\src\main\AndroidManifest.xml"
$m = [System.IO.File]::ReadAllText($manifest)
# GPS: never require hardware GPS
$m = [regex]::Replace($m, '(?m)^\s*<uses-feature android:name="android\.hardware\.location\.gps"[^/]*/>\s*\r?\n?', '')
if ($m -notmatch 'android\.hardware\.location\.gps') {
  $m = $m.Replace('</manifest>', "    <uses-feature android:name=`"android.hardware.location.gps`" android:required=`"false`" />`r`n</manifest>")
}
# Exact alarms: keep a single SCHEDULE_EXACT_ALARM (maxSdk 32) + USE_EXACT_ALARM
$m = [regex]::Replace($m, '(?m)^\s*<uses-permission[^>]*android\.permission\.SCHEDULE_EXACT_ALARM[^/]*/>\s*\r?\n?', '')
$m = [regex]::Replace($m, '(?m)^\s*<uses-permission[^>]*android\.permission\.USE_EXACT_ALARM[^/]*/>\s*\r?\n?', '')
$m = [regex]::Replace($m, '(?m)^\s*<uses-permission[^>]*android\.permission\.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS[^/]*/>\s*\r?\n?', '')
$permBlock = @"
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
"@
if ($m -match '</manifest>') {
  $m = $m.Replace('</manifest>', $permBlock + "`r`n</manifest>")
}
[System.IO.File]::WriteAllText($manifest, $m, $utf8NoBom)

Set-Location -LiteralPath (Join-Path $root "platforms\android")
.\gradlew.bat cdvBuildDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$apk = Get-ChildItem -LiteralPath (Join-Path $root "platforms\android\app\build\outputs\apk") -Recurse -Filter *.apk | Select-Object -First 1
$out1 = Join-Path $root "AlKawthar-debug.apk"
$out2 = Join-Path $parent ([char]0x0627 + [char]0x0644 + [char]0x0643 + [char]0x0648 + [char]0x062B + [char]0x0631 + '.apk')
$out2b = Join-Path $parent "AlKawthar.apk"
Copy-Item -LiteralPath $apk.FullName -Destination $out1 -Force
Copy-Item -LiteralPath $apk.FullName -Destination $out2b -Force
try { Copy-Item -LiteralPath $apk.FullName -Destination $out2 -Force } catch {}
Write-Host ""
Write-Host ("BUILD OK size=" + $apk.Length)
Write-Host ("  " + $out1)
Write-Host ("  " + $out2b)
