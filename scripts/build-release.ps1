param(
    [ValidateRange(0, 2100000000)]
    [int]$VersionCode = 0
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$wrapper = Join-Path $projectRoot 'gradlew.bat'
Set-Location -LiteralPath $projectRoot

function ConvertTo-ProcessSecret {
    param([Parameter(Mandatory = $true)][System.Security.SecureString]$Value)
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

$environmentNames = @(
    'ASTORY_RELEASE_STORE_FILE',
    'ASTORY_RELEASE_STORE_PASSWORD',
    'ASTORY_RELEASE_KEY_ALIAS',
    'ASTORY_RELEASE_KEY_PASSWORD'
)
$previousEnvironment = @{}
foreach ($name in $environmentNames) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$storePasswordSecure = $null
$keyPasswordSecure = $null
$storePassword = $null
$keyPassword = $null

try {
    $storeFile = Read-Host 'Đường dẫn tuyệt đối tới release keystore ngoài repository'
    $keyAlias = Read-Host 'Alias của khóa release'
    $storePasswordSecure = Read-Host 'Mật khẩu keystore' -AsSecureString
    $keyPasswordSecure = Read-Host 'Mật khẩu private key'
    if ($storePasswordSecure.Length -eq 0 -or $keyPasswordSecure.Length -eq 0) {
        throw 'Mật khẩu keystore và private key không được để trống.'
    }

    $storePassword = ConvertTo-ProcessSecret -Value $storePasswordSecure
    $keyPassword = ConvertTo-ProcessSecret -Value $keyPasswordSecure
    $env:ASTORY_RELEASE_STORE_FILE = $storeFile
    $env:ASTORY_RELEASE_STORE_PASSWORD = $storePassword
    $env:ASTORY_RELEASE_KEY_ALIAS = $keyAlias
    $env:ASTORY_RELEASE_KEY_PASSWORD = $keyPassword

    $gradleArguments = @(':app:assembleRelease', '--no-configuration-cache', '--console=plain')
    if ($VersionCode -gt 0) { $gradleArguments += "-Papp.versionCode=$VersionCode" }
    & $wrapper @gradleArguments
    if ($LASTEXITCODE -ne 0) { throw "Gradle release build failed with exit code $LASTEXITCODE." }

    $releaseApk = Join-Path $projectRoot 'app\build\outputs\apk\release\app-release.apk'
    if (-not (Test-Path -LiteralPath $releaseApk -PathType Leaf)) {
        throw 'Gradle completed without the expected release APK.'
    }
    $sdkRoot = $env:ANDROID_SDK_ROOT
    if ([string]::IsNullOrWhiteSpace($sdkRoot)) { $sdkRoot = $env:ANDROID_HOME }
    if ([string]::IsNullOrWhiteSpace($sdkRoot)) { $sdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
    $apksigner = Get-ChildItem (Join-Path $sdkRoot 'build-tools') -Directory |
        Sort-Object { [version]$_.Name } -Descending |
        ForEach-Object { Join-Path $_.FullName 'apksigner.bat' } |
        Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } |
        Select-Object -First 1
    if (-not $apksigner) { throw 'Android SDK apksigner.bat was not found.' }
    & $apksigner verify --verbose $releaseApk *> $null
    if ($LASTEXITCODE -ne 0) { throw 'apksigner rejected the generated release APK.' }

    Write-Host 'Release APK build and signature verification completed.'
} finally {
    foreach ($name in $environmentNames) {
        $previousValue = $previousEnvironment[$name]
        if ($null -eq $previousValue) {
            Remove-Item "Env:$name" -ErrorAction SilentlyContinue
        } else {
            [Environment]::SetEnvironmentVariable($name, $previousValue, 'Process')
        }
    }
    if ($null -ne $storePasswordSecure) { $storePasswordSecure.Dispose() }
    if ($null -ne $keyPasswordSecure) { $keyPasswordSecure.Dispose() }
    $storePassword = $null
    $keyPassword = $null
}
