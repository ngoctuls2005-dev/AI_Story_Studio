param(
    [Parameter(Mandatory = $true)]
    [string]$DeviceSerial
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Test-Common.ps1')
Initialize-A06Environment

$signingEnvironmentNames = @(
    'ASTORY_RELEASE_STORE_FILE',
    'ASTORY_RELEASE_STORE_PASSWORD',
    'ASTORY_RELEASE_KEY_ALIAS',
    'ASTORY_RELEASE_KEY_PASSWORD'
)
$previousSigningEnvironment = @{}
foreach ($name in $signingEnvironmentNames) {
    $previousSigningEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$storePasswordSecure = $null
$keyPasswordSecure = $null
$storePassword = $null
$keyPassword = $null

function ConvertTo-D06ProcessSecret {
    param([Parameter(Mandatory = $true)][System.Security.SecureString]$Value)
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

try {
    if ([string]::IsNullOrWhiteSpace($env:ASTORY_RELEASE_STORE_FILE)) {
        $env:ASTORY_RELEASE_STORE_FILE = Read-Host 'Đường dẫn tuyệt đối tới release keystore ngoài repository'
    }
    if ([string]::IsNullOrWhiteSpace($env:ASTORY_RELEASE_KEY_ALIAS)) {
        $env:ASTORY_RELEASE_KEY_ALIAS = Read-Host 'Alias của khóa release'
    }
    if ([string]::IsNullOrWhiteSpace($env:ASTORY_RELEASE_STORE_PASSWORD)) {
        $storePasswordSecure = Read-Host 'Mật khẩu keystore' -AsSecureString
        if ($storePasswordSecure.Length -eq 0) { throw 'Mật khẩu keystore không được để trống.' }
        $storePassword = ConvertTo-D06ProcessSecret -Value $storePasswordSecure
        $env:ASTORY_RELEASE_STORE_PASSWORD = $storePassword
    }
    if ([string]::IsNullOrWhiteSpace($env:ASTORY_RELEASE_KEY_PASSWORD)) {
        $keyPasswordSecure = Read-Host 'Mật khẩu private key' -AsSecureString
        if ($keyPasswordSecure.Length -eq 0) { throw 'Mật khẩu private key không được để trống.' }
        $keyPassword = ConvertTo-D06ProcessSecret -Value $keyPasswordSecure
        $env:ASTORY_RELEASE_KEY_PASSWORD = $keyPassword
    }

$requiredSigningVariables = @(
    'ASTORY_RELEASE_STORE_FILE',
    'ASTORY_RELEASE_STORE_PASSWORD',
    'ASTORY_RELEASE_KEY_ALIAS',
    'ASTORY_RELEASE_KEY_PASSWORD'
)
$missingVariables = @($requiredSigningVariables | Where-Object {
    [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, 'Process'))
})
if ($missingVariables.Count -gt 0) {
    throw "Missing release signing environment variables: $($missingVariables -join ', ')."
}

$storePath = [Environment]::GetEnvironmentVariable('ASTORY_RELEASE_STORE_FILE', 'Process')
if (-not [System.IO.Path]::IsPathRooted($storePath)) {
    throw 'Release keystore must use an absolute path outside the repository.'
}
$canonicalStorePath = [System.IO.Path]::GetFullPath($storePath)
$canonicalProjectPath = [System.IO.Path]::GetFullPath($script:A06ProjectRoot).TrimEnd('\') + '\'
if ($canonicalStorePath.StartsWith($canonicalProjectPath, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Release keystore must be stored outside the repository.'
}
if (-not (Test-Path -LiteralPath $canonicalStorePath -PathType Leaf)) {
    throw 'Configured release keystore does not exist or is not a file.'
}

$device = Select-A06Emulator -DeviceSerial $DeviceSerial
$appId = 'com.hoangtu.aistorystudio'
$installed = Invoke-A06Adb -Serial $device.Serial -Arguments @('shell', 'cmd', 'package', 'list', 'packages', $appId) -LogName 'd06-preflight-app' -Quiet
if ($installed.Output -match '(?m)^package:') {
    throw 'The release test emulator already contains this application. Use a fresh isolated emulator; this script never uninstalls it.'
}

$versionLine = Get-Content (Join-Path $script:A06ProjectRoot 'gradle.properties') |
    Where-Object { $_ -match '^\s*app\.versionCode\s*=' } |
    Select-Object -Last 1
if ($versionLine -notmatch '^\s*app\.versionCode\s*=\s*(\d+)\s*$') {
    throw 'gradle.properties must define a numeric app.versionCode for release V1.'
}
$versionV1 = [int]$Matches[1]
if ($versionV1 -ge 2100000000) { throw 'No higher Android versionCode is available for the V2 test.' }
$versionV2 = $versionV1 + 1

$runId = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$runDirectory = Join-Path $script:A06ReportDirectory "$runId-release-upgrade"
New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null
$outputDirectories = @(
    (Join-Path $script:A06ProjectRoot 'app\build\outputs\apk\release'),
    (Join-Path $script:A06ProjectRoot 'app\build\outputs\apk\androidTest\debug')
)
$outputBackup = Join-Path $runDirectory 'previous-release-outputs'
$existingOutputDirectories = @{}
foreach ($directory in $outputDirectories) {
    $existingOutputDirectories[$directory] = Test-Path -LiteralPath $directory -PathType Container
    if ($existingOutputDirectories[$directory]) {
        $relative = [System.IO.Path]::GetRelativePath((Join-Path $script:A06ProjectRoot 'app\build\outputs\apk'), $directory)
        $backupDirectory = Join-Path $outputBackup $relative
        New-Item -ItemType Directory -Path (Split-Path -Parent $backupDirectory) -Force | Out-Null
        Copy-Item -LiteralPath $directory -Destination $backupDirectory -Recurse -Force
    }
}

$seedStarted = $false
$fixtureProviderId = 'a06-release-upgrade-validation'
$instrumentationClass = 'com.hoangtu.aistorystudio.D06ApkUpgradeInstrumentedTest'
$testPackageRunner = 'com.hoangtu.aistorystudio.test/androidx.test.runner.AndroidJUnitRunner'
$script:D06LogsToScan = @()

function Invoke-D06ReleaseInstrumentation {
    param([Parameter(Mandatory = $true)][string]$Phase)
    $result = Invoke-A06Adb -Serial $device.Serial -Arguments @(
        'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $instrumentationClass,
        '-e', 'phase', $Phase, $testPackageRunner
    ) -LogName "d06-release-$Phase" -Quiet
    Assert-A06InstrumentationOutput -Output $result.Output
    $script:D06LogsToScan += $result.LogPath
}

function Get-D06ApkMetadata {
    param([Parameter(Mandatory = $true)][string]$ApkPath, [Parameter(Mandatory = $true)][int]$ExpectedVersionCode)
    $badging = @(& $script:A06Aapt dump badging $ApkPath 2>&1)
    if ($LASTEXITCODE -ne 0) { throw 'aapt could not inspect a release APK.' }
    $metadataMatch = [regex]::Match(($badging -join "`n"), "package: name='([^']+)' versionCode='(\d+)' versionName='([^']*)'")
    if (-not $metadataMatch.Success) { throw 'Release APK manifest metadata could not be parsed.' }
    if ($metadataMatch.Groups[1].Value -ne $appId) { throw 'Release APK applicationId does not match the established applicationId.' }
    if ([int]$metadataMatch.Groups[2].Value -ne $ExpectedVersionCode) { throw 'Release APK versionCode does not match the requested version.' }

    $signature = @(& $script:A06ApkSigner verify --print-certs --verbose $ApkPath 2>&1)
    if ($LASTEXITCODE -ne 0) { throw 'apksigner rejected a release APK.' }
    $fingerprintMatch = [regex]::Match(($signature -join "`n"), 'Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F:]+)')
    if (-not $fingerprintMatch.Success) { throw 'apksigner did not report a signer certificate fingerprint.' }

    [pscustomobject]@{
        ApplicationId = $metadataMatch.Groups[1].Value
        VersionCode = [int]$metadataMatch.Groups[2].Value
        CertificateSha256 = ($fingerprintMatch.Groups[1].Value -replace ':', '').ToUpperInvariant()
    }
}

function Assert-D06NoSigningSecretInText {
    param([Parameter(Mandatory = $true)][string]$Path, [Parameter(Mandatory = $true)][string[]]$Secrets)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Expected security scan input is missing: $Path" }
    $content = [System.IO.File]::ReadAllText($Path)
    foreach ($secret in $Secrets) {
        if (-not [string]::IsNullOrEmpty($secret) -and $content.Contains($secret)) {
            throw 'A signing password was found in a build log or repository file.'
        }
    }
}

function Assert-D06NoSigningSecretInApk {
    param([Parameter(Mandatory = $true)][string]$ApkPath, [Parameter(Mandatory = $true)][string[]]$Secrets)
    $archive = [System.IO.Compression.ZipFile]::OpenRead($ApkPath)
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName -match '(?i)(\.jks$|\.keystore$|\.p12$|\.pfx$|key\.properties$|secrets\.properties$)') {
                throw 'A release APK contains a keystore or signing configuration file.'
            }
            $stream = $entry.Open()
            $reader = [System.IO.StreamReader]::new($stream, [System.Text.Encoding]::UTF8, $true)
            try { $content = $reader.ReadToEnd() } finally { $reader.Dispose() }
            foreach ($secret in $Secrets) {
                if (-not [string]::IsNullOrEmpty($secret) -and $content.Contains($secret)) {
                    throw 'A signing password was found in a release APK entry.'
                }
            }
        }
    } finally {
        $archive.Dispose()
    }
}

try {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $sdkRoot = $env:ANDROID_SDK_ROOT
    $buildToolsDirectory = Get-ChildItem (Join-Path $sdkRoot 'build-tools') -Directory |
        Sort-Object { [version]$_.Name } -Descending |
        Select-Object -First 1
    $script:A06Aapt = Join-Path $buildToolsDirectory.FullName 'aapt.exe'
    $script:A06ApkSigner = Join-Path $buildToolsDirectory.FullName 'apksigner.bat'
    if (-not (Test-Path -LiteralPath $script:A06Aapt -PathType Leaf) -or -not (Test-Path -LiteralPath $script:A06ApkSigner -PathType Leaf)) {
        throw 'Required Android SDK aapt or apksigner tool was not found.'
    }

    $debugApk = Join-Path $script:A06ProjectRoot 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path -LiteralPath $debugApk -PathType Leaf)) { throw 'Build the debug APK before the release upgrade test.' }
    $debugSignature = @(& $script:A06ApkSigner verify --print-certs --verbose $debugApk 2>&1)
    if ($LASTEXITCODE -ne 0) { throw 'apksigner rejected the debug APK.' }
    $debugFingerprintMatch = [regex]::Match(($debugSignature -join "`n"), 'Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F:]+)')
    if (-not $debugFingerprintMatch.Success) { throw 'apksigner did not report the debug certificate fingerprint.' }
    $debugFingerprint = ($debugFingerprintMatch.Groups[1].Value -replace ':', '').ToUpperInvariant()

    $buildV1 = Invoke-A06Gradle -Arguments @(
        ':app:assembleRelease', ':app:assembleDebugAndroidTest', "-Papp.versionCode=$versionV1",
        '--no-configuration-cache', '--rerun-tasks', '--console=plain'
    ) -LogName 'd06-release-v1-build'
    $script:D06LogsToScan += $buildV1.LogPath
    if ($buildV1.ExitCode -ne 0) { throw 'Release V1 build failed; inspect the redacted Gradle log.' }
    $v1ApkPath = Join-Path $script:A06ProjectRoot 'app\build\outputs\apk\release\app-release.apk'
    $v1TestApkPath = Join-Path $script:A06ProjectRoot 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
    if (-not (Test-Path -LiteralPath $v1ApkPath -PathType Leaf) -or -not (Test-Path -LiteralPath $v1TestApkPath -PathType Leaf)) {
        throw 'Release V1 app or instrumentation APK was not generated.'
    }
    $v1ApkSnapshot = Join-Path $runDirectory 'release-v1.apk'
    $v1TestApkSnapshot = Join-Path $runDirectory 'release-v1-androidTest.apk'
    Copy-Item -LiteralPath $v1ApkPath -Destination $v1ApkSnapshot
    Copy-Item -LiteralPath $v1TestApkPath -Destination $v1TestApkSnapshot
    $testSignOutput = @(& $script:A06ApkSigner sign --ks $canonicalStorePath --ks-key-alias $env:ASTORY_RELEASE_KEY_ALIAS --ks-pass 'env:ASTORY_RELEASE_STORE_PASSWORD' --key-pass 'env:ASTORY_RELEASE_KEY_PASSWORD' $v1TestApkSnapshot 2>&1)
    if ($LASTEXITCODE -ne 0) { throw 'Could not sign the isolated instrumentation APK with the release test certificate.' }
    $v1Metadata = Get-D06ApkMetadata -ApkPath $v1ApkSnapshot -ExpectedVersionCode $versionV1
    if ($v1Metadata.CertificateSha256 -eq $debugFingerprint) {
        throw 'Release and debug APKs use the same certificate; signing keys must remain separate.'
    }
    Assert-D06NoSigningSecretInApk -ApkPath $v1ApkSnapshot -Secrets @(
        $env:ASTORY_RELEASE_STORE_PASSWORD, $env:ASTORY_RELEASE_KEY_PASSWORD
    )
    $testSignature = @(& $script:A06ApkSigner verify --print-certs --verbose $v1TestApkSnapshot 2>&1)
    if ($LASTEXITCODE -ne 0) { throw 'apksigner rejected the release-signed instrumentation APK.' }
    $testFingerprintMatch = [regex]::Match(($testSignature -join "`n"), 'Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F:]+)')
    if (-not $testFingerprintMatch.Success -or ($testFingerprintMatch.Groups[1].Value -replace ':', '').ToUpperInvariant() -ne $v1Metadata.CertificateSha256) {
        throw 'The release instrumentation APK does not use the same certificate as the release app.'
    }
    Assert-D06NoSigningSecretInApk -ApkPath $v1TestApkSnapshot -Secrets @(
        $env:ASTORY_RELEASE_STORE_PASSWORD, $env:ASTORY_RELEASE_KEY_PASSWORD
    )

    $v1Install = Invoke-A06Adb -Serial $device.Serial -Arguments @('install', '-r', $v1ApkSnapshot) -LogName 'd06-release-v1-install' -Quiet
    if ($v1Install.Output -notmatch 'Success') { throw 'Release V1 did not install successfully on the fresh emulator.' }
    $testInstall = Invoke-A06Adb -Serial $device.Serial -Arguments @('install', '-r', $v1TestApkSnapshot) -LogName 'd06-release-v1-test-install' -Quiet
    if ($testInstall.Output -notmatch 'Success') { throw 'Release V1 instrumentation APK did not install.' }
    $seedStarted = $true
    Invoke-D06ReleaseInstrumentation -Phase 'seed'

    $buildV2 = Invoke-A06Gradle -Arguments @(
        ':app:assembleRelease', "-Papp.versionCode=$versionV2",
        '--no-configuration-cache', '--rerun-tasks', '--console=plain'
    ) -LogName 'd06-release-v2-build'
    $script:D06LogsToScan += $buildV2.LogPath
    if ($buildV2.ExitCode -ne 0) { throw 'Release V2 build failed; inspect the redacted Gradle log.' }
    $v2ApkPath = Join-Path $script:A06ProjectRoot 'app\build\outputs\apk\release\app-release.apk'
    if (-not (Test-Path -LiteralPath $v2ApkPath -PathType Leaf)) {
        throw 'Release V2 app APK was not generated.'
    }
    $v2ApkSnapshot = Join-Path $runDirectory 'release-v2.apk'
    $v2TestApkSnapshot = Join-Path $runDirectory 'release-v2-androidTest.apk'
    Copy-Item -LiteralPath $v2ApkPath -Destination $v2ApkSnapshot
    Copy-Item -LiteralPath $v1TestApkSnapshot -Destination $v2TestApkSnapshot
    $v2Metadata = Get-D06ApkMetadata -ApkPath $v2ApkSnapshot -ExpectedVersionCode $versionV2
    if ($v1Metadata.ApplicationId -ne $v2Metadata.ApplicationId -or $v1Metadata.CertificateSha256 -ne $v2Metadata.CertificateSha256) {
        throw 'V1 and V2 do not share the applicationId and signer certificate.'
    }
    if ($v2Metadata.VersionCode -le $v1Metadata.VersionCode) { throw 'V2 versionCode must exceed V1.' }
    Assert-D06NoSigningSecretInApk -ApkPath $v2ApkSnapshot -Secrets @(
        $env:ASTORY_RELEASE_STORE_PASSWORD, $env:ASTORY_RELEASE_KEY_PASSWORD
    )
    Assert-D06NoSigningSecretInApk -ApkPath $v2TestApkSnapshot -Secrets @(
        $env:ASTORY_RELEASE_STORE_PASSWORD, $env:ASTORY_RELEASE_KEY_PASSWORD
    )

    $v2Install = Invoke-A06Adb -Serial $device.Serial -Arguments @('install', '-r', $v2ApkSnapshot) -LogName 'd06-release-v2-install' -Quiet
    if ($v2Install.Output -notmatch 'Success') { throw 'Release V2 was not accepted as an in-place APK update.' }
    $v2TestInstall = Invoke-A06Adb -Serial $device.Serial -Arguments @('install', '-r', $v2TestApkSnapshot) -LogName 'd06-release-v2-test-install' -Quiet
    if ($v2TestInstall.Output -notmatch 'Success') { throw 'Release V2 instrumentation APK did not install as an update.' }
    Invoke-D06ReleaseInstrumentation -Phase 'verify'
    Invoke-D06ReleaseInstrumentation -Phase 'cleanup'
    $seedStarted = $false

    $repositoryFiles = @(& git -C $script:A06ProjectRoot ls-files --cached --others --exclude-standard)
    if ($LASTEXITCODE -ne 0) { throw 'Could not enumerate tracked and untracked repository files for the signing-secret scan.' }
    $repositorySigningFiles = @($repositoryFiles | Where-Object {
        $_ -match '(?i)(^|/)(local\.properties|key\.properties|secrets\.properties|release-signing\.properties)$' -or
            $_ -match '(?i)\.(jks|keystore|p12|pfx|pem|key)$'
    })
    if ($repositorySigningFiles.Count -gt 0) {
        throw 'A keystore or signing configuration file is present in the Git working tree.'
    }
    foreach ($relativePath in $repositoryFiles) {
        $absolutePath = Join-Path $script:A06ProjectRoot $relativePath
        if (Test-Path -LiteralPath $absolutePath -PathType Leaf) {
            Assert-D06NoSigningSecretInText -Path $absolutePath -Secrets @(
                $env:ASTORY_RELEASE_STORE_PASSWORD, $env:ASTORY_RELEASE_KEY_PASSWORD
            )
        }
    }
    foreach ($logPath in $script:D06LogsToScan) {
        Assert-D06NoSigningSecretInText -Path $logPath -Secrets @(
            $env:ASTORY_RELEASE_STORE_PASSWORD, $env:ASTORY_RELEASE_KEY_PASSWORD
        )
    }

    Write-Host "Release upgrade PASS: applicationId=$($v1Metadata.ApplicationId), versionCode=$($v1Metadata.VersionCode)->$($v2Metadata.VersionCode), certificate=$($v1Metadata.CertificateSha256)."
    Write-Host "V1/V2 and test APK snapshots: $runDirectory"
} finally {
    if ($seedStarted) {
        try { Invoke-D06ReleaseInstrumentation -Phase 'cleanup' } catch { Write-Warning 'Could not confirm cleanup of the reserved release upgrade fixture.' }
    }
    $apkOutputRoot = Join-Path $script:A06ProjectRoot 'app\build\outputs\apk'
    foreach ($directory in $outputDirectories) {
        $canonicalDirectory = [System.IO.Path]::GetFullPath($directory)
        $canonicalRoot = [System.IO.Path]::GetFullPath($apkOutputRoot).TrimEnd('\') + '\'
        if (-not $canonicalDirectory.StartsWith($canonicalRoot, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'Refusing to restore release outputs outside app/build/outputs/apk.'
        }
        if ($existingOutputDirectories[$directory]) {
            if (Test-Path -LiteralPath $directory -PathType Container) {
                Remove-Item -LiteralPath $directory -Recurse -Force
            }
            $relative = [System.IO.Path]::GetRelativePath($apkOutputRoot, $directory)
            $backupDirectory = Join-Path $outputBackup $relative
            Copy-Item -LiteralPath $backupDirectory -Destination $directory -Recurse -Force
        } elseif (Test-Path -LiteralPath $directory -PathType Container) {
            Remove-Item -LiteralPath $directory -Recurse -Force
        }
    }
}
} finally {
    foreach ($name in $signingEnvironmentNames) {
        $previousValue = $previousSigningEnvironment[$name]
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
