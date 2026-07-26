$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$gradleWrapper = Join-Path $projectRoot "gradlew.bat"
$gradleFile = Join-Path $projectRoot "app\build.gradle.kts"

if (-not (Test-Path $gradleWrapper)) {
    throw "Could not find gradlew.bat in the project root."
}

if (-not (Test-Path $gradleFile)) {
    throw "Could not find app\build.gradle.kts."
}

$gradleContent = Get-Content $gradleFile -Raw

$versionNameMatch = [regex]::Match(
    $gradleContent,
    'versionName\s*=\s*"([^"]+)"'
)

$versionCodeMatch = [regex]::Match(
    $gradleContent,
    'versionCode\s*=\s*(\d+)'
)

if (-not $versionNameMatch.Success) {
    throw "Could not find versionName in app\build.gradle.kts."
}

if (-not $versionCodeMatch.Success) {
    throw "Could not find versionCode in app\build.gradle.kts."
}

$versionName = $versionNameMatch.Groups[1].Value
$versionCode = $versionCodeMatch.Groups[1].Value

Write-Host ""
Write-Host "Building Frame v$versionName ($versionCode)..."
Write-Host ""

Push-Location $projectRoot

try {
    & $gradleWrapper clean assembleRelease bundleRelease

    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

$apkSource = Join-Path $projectRoot "app\release\app-release.apk"
$aabSource = Join-Path $projectRoot "app\release\app-release.aab"

if (-not (Test-Path $apkSource)) {
    throw "Release APK not found at: $apkSource"
}

if (-not (Test-Path $aabSource)) {
    throw "Release AAB not found at: $aabSource"
}

$outputDirectory = Join-Path $projectRoot "releases"
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

$apkDestination = Join-Path $outputDirectory "Frame-v$versionName-$versionCode.apk"
$aabDestination = Join-Path $outputDirectory "Frame-v$versionName-$versionCode.aab"

Copy-Item $apkSource $apkDestination -Force
Copy-Item $aabSource $aabDestination -Force

Write-Host ""
Write-Host "Release build completed successfully."
Write-Host ""
Write-Host "Artifacts:"
Write-Host "  $apkDestination"
Write-Host "  $aabDestination"