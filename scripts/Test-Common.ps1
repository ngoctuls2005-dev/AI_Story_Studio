$script:A06ProjectRoot = Split-Path -Parent $PSScriptRoot
$script:A06GradleWrapper = Join-Path $script:A06ProjectRoot 'gradlew.bat'
$script:A06ReportDirectory = Join-Path $script:A06ProjectRoot 'app\build\reports\a06'
$script:A06RunId = Get-Date -Format 'yyyyMMdd-HHmmss-fff'

function Initialize-A06Environment {
    Set-Location -LiteralPath $script:A06ProjectRoot

    if (-not (Test-Path -LiteralPath $script:A06GradleWrapper)) {
        throw "Khong tim thay Gradle Wrapper: $script:A06GradleWrapper"
    }

    $javaHome = $env:JAVA_HOME
    if ([string]::IsNullOrWhiteSpace($javaHome) -or -not (Test-Path -LiteralPath (Join-Path $javaHome 'bin\java.exe'))) {
        $javaHome = 'C:\Program Files\Android\Android Studio\jbr'
    }

    $javaExecutable = Join-Path $javaHome 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $javaExecutable)) {
        throw 'Can JDK 25 (Android Studio JBR) de chay Gradle daemon cua du an.'
    }

    $javaVersion = (& $javaExecutable -version 2>&1 | Out-String)
    if ($javaVersion -notmatch 'version "25\.') {
        throw "Gradle cua du an can JDK 25; dang phat hien: $($javaVersion.Trim())"
    }
    $env:JAVA_HOME = $javaHome
    $env:PATH = "$javaHome\bin;$env:PATH"

    $sdkRoot = $env:ANDROID_SDK_ROOT
    if ([string]::IsNullOrWhiteSpace($sdkRoot)) { $sdkRoot = $env:ANDROID_HOME }
    if ([string]::IsNullOrWhiteSpace($sdkRoot)) { $sdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
    $adbPath = Join-Path $sdkRoot 'platform-tools\adb.exe'
    if (-not (Test-Path -LiteralPath $adbPath)) {
        throw "Khong tim thay Android SDK platform-tools/adb.exe tai: $sdkRoot"
    }
    $env:ANDROID_SDK_ROOT = $sdkRoot
    $env:ANDROID_HOME = $sdkRoot
    $script:A06Adb = $adbPath

    $shortTemp = $env:A06_GRADLE_TEMP
    if ([string]::IsNullOrWhiteSpace($shortTemp)) { $shortTemp = 'C:\jtmp' }
    if (-not (Test-Path -LiteralPath $shortTemp)) {
        New-Item -ItemType Directory -Path $shortTemp -Force | Out-Null
    }
    $env:TEMP = $shortTemp
    $env:TMP = $shortTemp

    $env:ANDROID_ADB_SERVER_PORT = '5037'
    $adbStart = & $script:A06Adb start-server 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Khong khoi dong duoc ADB server cong 5037: $($adbStart -join ' ')"
    }

    New-Item -ItemType Directory -Path $script:A06ReportDirectory -Force | Out-Null
    Write-Host "Project: $script:A06ProjectRoot"
    Write-Host "JDK: $javaHome (25)"
    Write-Host "Android SDK: $sdkRoot"
    Write-Host "ADB server: port 5037"
    Write-Host "Test run: $script:A06RunId"
}

function New-A06LogPath {
    param([Parameter(Mandatory = $true)][string]$Name)
    Join-Path $script:A06ReportDirectory "$($script:A06RunId)-$Name.log"
}

function Invoke-A06Gradle {
    param([Parameter(Mandatory = $true)][string[]]$Arguments, [Parameter(Mandatory = $true)][string]$LogName)

    $startedUtc = [DateTime]::UtcNow
    $logPath = New-A06LogPath -Name $LogName
    Write-Host "Gradle bat dau: $($Arguments -join ' ')"
    $lines = @(& $script:A06GradleWrapper @Arguments 2>&1)
    $exitCode = $LASTEXITCODE
    $lines | ForEach-Object { [string]$_ } | Set-Content -LiteralPath $logPath -Encoding utf8
    $lines | Select-Object -Last 35 | ForEach-Object { Write-Host ([string]$_) }
    Write-Host "Gradle log: $logPath"

    [pscustomobject]@{
        ExitCode = $exitCode
        StartedUtc = $startedUtc
        LogPath = $logPath
        Output = ($lines -join [Environment]::NewLine)
    }
}

function Get-A06JUnitSummary {
    param([Parameter(Mandatory = $true)][string]$ResultsDirectory, [Parameter(Mandatory = $true)][DateTime]$StartedUtc)

    $reports = @()
    if (Test-Path -LiteralPath $ResultsDirectory) {
        $reports = @(Get-ChildItem -LiteralPath $ResultsDirectory -Filter 'TEST-*.xml' -File |
            Where-Object { $_.LastWriteTimeUtc -ge $StartedUtc })
    }

    $tests = 0
    $failures = 0
    $errors = 0
    $skipped = 0
    foreach ($report in $reports) {
        try {
            [xml]$xml = Get-Content -LiteralPath $report.FullName -Raw
        } catch {
            throw "JUnit XML khong hop le: $($report.FullName)"
        }
        foreach ($suite in $xml.SelectNodes('//testsuite')) {
            $tests += [int]$suite.GetAttribute('tests')
            $failures += [int]$suite.GetAttribute('failures')
            $errors += [int]$suite.GetAttribute('errors')
            $skipped += [int]$suite.GetAttribute('skipped')
        }
    }

    [pscustomobject]@{
        ReportCount = $reports.Count
        Tests = $tests
        Failures = $failures
        Errors = $errors
        Skipped = $skipped
        ReportPaths = @($reports | ForEach-Object { $_.FullName })
    }
}

function Assert-A06TestCounts {
    param([Parameter(Mandatory = $true)]$Summary, [Parameter(Mandatory = $true)][string]$SuiteName)

    Write-Host "$SuiteName`: reports=$($Summary.ReportCount), tests=$($Summary.Tests), failures=$($Summary.Failures), errors=$($Summary.Errors), skipped=$($Summary.Skipped)"
    if ($Summary.ReportCount -eq 0) { throw "$SuiteName khong tao report moi; khong the xac nhan test da chay." }
    if ($Summary.Tests -le 0) { throw "$SuiteName co 0 test; tu choi bao PASS." }
    if ($Summary.Failures -ne 0 -or $Summary.Errors -ne 0 -or $Summary.Skipped -ne 0) {
        throw "$SuiteName co failure/error/skip; ket qua khong dat."
    }
    foreach ($path in $Summary.ReportPaths) { Write-Host "JUnit: $path" }
}

function Invoke-A06Adb {
    param([Parameter(Mandatory = $true)][string]$Serial, [Parameter(Mandatory = $true)][string[]]$Arguments, [Parameter(Mandatory = $true)][string]$LogName, [switch]$Quiet)

    $logPath = New-A06LogPath -Name $LogName
    $lines = @(& $script:A06Adb -s $Serial @Arguments 2>&1)
    $exitCode = $LASTEXITCODE
    $lines | ForEach-Object { [string]$_ } | Set-Content -LiteralPath $logPath -Encoding utf8
    if (-not $Quiet.IsPresent) { $lines | ForEach-Object { Write-Host ([string]$_) } }
    if ($exitCode -ne 0) { throw "ADB command that bai ($exitCode). Log: $logPath" }
    [pscustomobject]@{ Output = ($lines -join [Environment]::NewLine); LogPath = $logPath }
}

function Select-A06Emulator {
    param([string]$DeviceSerial)

    $deviceOutput = @(& $script:A06Adb devices -l 2>&1)
    if ($LASTEXITCODE -ne 0) { throw "Khong doc duoc danh sach ADB: $($deviceOutput -join ' ')" }
    $online = @()
    foreach ($line in $deviceOutput) {
        if ([string]$line -match '^\s*(\S+)\s+device(?:\s|$)') {
            $online += $Matches[1]
        }
    }
    if ($online.Count -eq 0) { throw 'Khong co Android device/emulator online. Khong co test instrument nao duoc chay.' }
    if ([string]::IsNullOrWhiteSpace($DeviceSerial)) {
        if ($online.Count -ne 1) {
            throw "Co nhieu device online ($($online -join ', ')); chay lai voi -DeviceSerial va chon emulator."
        }
        $DeviceSerial = $online[0]
    }
    if ($online -notcontains $DeviceSerial) {
        throw "Serial '$DeviceSerial' khong nam trong danh sach device online: $($online -join ', ')"
    }

    $qemu = Invoke-A06Adb -Serial $DeviceSerial -Arguments @('shell', 'getprop', 'ro.kernel.qemu') -LogName 'device-qemu' -Quiet
    if ($qemu.Output.Trim() -ne '1') {
        throw "Tu choi chay test tren '$DeviceSerial': day khong phai emulator. Script chi cai APK test vao emulator."
    }
    $api = Invoke-A06Adb -Serial $DeviceSerial -Arguments @('shell', 'getprop', 'ro.build.version.sdk') -LogName 'device-api' -Quiet
    if ([int]$api.Output.Trim() -lt 26) { throw "Emulator API $($api.Output.Trim()) thap hon minSdk 26." }

    [pscustomobject]@{ Serial = $DeviceSerial; OnlineCount = $online.Count; OnlineSerials = $online; Api = $api.Output.Trim() }
}

function Invoke-A06DirectInstrumentation {
    param([Parameter(Mandatory = $true)][string]$Serial)

    $build = Invoke-A06Gradle -Arguments @(':app:assembleDebug', ':app:assembleDebugAndroidTest', '--no-daemon', '--console=plain') -LogName 'instrumentation-direct-build'
    if ($build.ExitCode -ne 0) { throw "Build APK instrumentation that bai ($($build.ExitCode)). Log: $($build.LogPath)" }

    $appApk = Join-Path $script:A06ProjectRoot 'app\build\outputs\apk\debug\app-debug.apk'
    $testApk = Join-Path $script:A06ProjectRoot 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
    foreach ($path in @($appApk, $testApk)) {
        if (-not (Test-Path -LiteralPath $path)) { throw "Khong tim thay APK instrumentation: $path" }
    }

    Write-Host "Cai app/test APK chi vao emulator $Serial; khong go app va khong xoa du lieu."
    Invoke-A06Adb -Serial $Serial -Arguments @('install', '-r', $appApk) -LogName 'install-app' | Out-Null
    Invoke-A06Adb -Serial $Serial -Arguments @('install', '-r', $testApk) -LogName 'install-test' | Out-Null
    Write-Host "AndroidJUnitRunner bat dau tren $Serial."
    $run = Invoke-A06Adb -Serial $Serial -Arguments @('shell', 'am', 'instrument', '-w', '-r', 'com.hoangtu.aistorystudio.test/androidx.test.runner.AndroidJUnitRunner') -LogName 'instrumentation-direct-runner' -Quiet
    Assert-A06InstrumentationOutput -Output $run.Output
    Write-Host "Instrumentation raw output: $($run.LogPath)"
    Invoke-A06ProcessRestartRegression -Serial $Serial
}

function Invoke-A06ProcessRestartRegression {
    param([Parameter(Mandatory = $true)][string]$Serial)

    $providerId = 'a06-test-process-restart'
    $runner = 'com.hoangtu.aistorystudio.test/androidx.test.runner.AndroidJUnitRunner'
    $testClass = 'com.hoangtu.aistorystudio.A05CredentialRestartInstrumentedTest'
    Write-Host "Process restart regression bat dau tren $Serial voi provider fixture rieng."
    try {
        $seed = Invoke-A06Adb -Serial $Serial -Arguments @(
            'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $testClass, '-e', 'phase', 'seed', '-e', 'providerId', $providerId, $runner
        ) -LogName 'process-restart-seed' -Quiet
        Assert-A06InstrumentationOutput -Output $seed.Output
        Invoke-A06Adb -Serial $Serial -Arguments @('shell', 'am', 'force-stop', 'com.hoangtu.aistorystudio') -LogName 'process-restart-force-stop' -Quiet | Out-Null
        Invoke-A06Adb -Serial $Serial -Arguments @(
            'shell', 'monkey', '-p', 'com.hoangtu.aistorystudio', '-c', 'android.intent.category.LAUNCHER', '1'
        ) -LogName 'process-restart-launch' -Quiet | Out-Null
        $verify = Invoke-A06Adb -Serial $Serial -Arguments @(
            'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $testClass, '-e', 'phase', 'verify', '-e', 'providerId', $providerId, $runner
        ) -LogName 'process-restart-verify' -Quiet
        Assert-A06InstrumentationOutput -Output $verify.Output
        $cleanup = Invoke-A06Adb -Serial $Serial -Arguments @(
            'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $testClass, '-e', 'phase', 'cleanup', '-e', 'providerId', $providerId, $runner
        ) -LogName 'process-restart-cleanup' -Quiet
        Assert-A06InstrumentationOutput -Output $cleanup.Output
        Write-Host "Process restart logs: $($seed.LogPath), $($verify.LogPath), $($cleanup.LogPath)"
        Invoke-C01PersonaRestartRegression -Serial $Serial
    } catch {
        Invoke-A06Adb -Serial $Serial -Arguments @(
            'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $testClass, '-e', 'phase', 'cleanup', '-e', 'providerId', $providerId, $runner
        ) -LogName 'process-restart-cleanup' -Quiet | Out-Null
        throw
    }
}

function Invoke-C01PersonaRestartRegression {
    param([Parameter(Mandatory = $true)][string]$Serial)

    $databaseName = "c01_restart_$([guid]::NewGuid().ToString('N')).db"
    $runner = 'com.hoangtu.aistorystudio.test/androidx.test.runner.AndroidJUnitRunner'
    $testClass = 'com.hoangtu.aistorystudio.feature.characters.C01PersonaRestartInstrumentedTest'
    $invokePhase = {
        param([string]$Phase, [string]$LogName)
        $result = Invoke-A06Adb -Serial $Serial -Arguments @(
            'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $testClass,
            '-e', 'phase', $Phase, '-e', 'databaseName', $databaseName, $runner
        ) -LogName $LogName -Quiet
        Assert-A06InstrumentationOutput -Output $result.Output
        $result
    }

    Write-Host "C-01 Room persistence restart test bat dau tren $Serial voi database test rieng."
    try {
        $seed = & $invokePhase 'seed' 'c01-restart-seed'
        Invoke-A06Adb -Serial $Serial -Arguments @('shell', 'am', 'force-stop', 'com.hoangtu.aistorystudio') -LogName 'c01-restart-force-stop' -Quiet | Out-Null
        Invoke-A06Adb -Serial $Serial -Arguments @(
            'shell', 'monkey', '-p', 'com.hoangtu.aistorystudio', '-c', 'android.intent.category.LAUNCHER', '1'
        ) -LogName 'c01-restart-launch' -Quiet | Out-Null
        $verify = & $invokePhase 'verify' 'c01-restart-verify'
        $cleanup = & $invokePhase 'cleanup' 'c01-restart-cleanup'
        Write-Host "C-01 process restart logs: $($seed.LogPath), $($verify.LogPath), $($cleanup.LogPath)"
    } catch {
        & $invokePhase 'cleanup' 'c01-restart-cleanup' | Out-Null
        throw
    }

    Invoke-C02ConversationRestartRegression -Serial $Serial
}

function Invoke-C02ConversationRestartRegression {
    param([Parameter(Mandatory = $true)][string]$Serial)

    $databaseName = "c02_restart_$([guid]::NewGuid().ToString('N')).db"
    $runner = 'com.hoangtu.aistorystudio.test/androidx.test.runner.AndroidJUnitRunner'
    $testClass = 'com.hoangtu.aistorystudio.feature.roleplay.C02ConversationRestartInstrumentedTest'
    $invokePhase = {
        param([string]$Phase, [string]$LogName)
        $result = Invoke-A06Adb -Serial $Serial -Arguments @(
            'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $testClass,
            '-e', 'phase', $Phase, '-e', 'databaseName', $databaseName, $runner
        ) -LogName $LogName -Quiet
        Assert-A06InstrumentationOutput -Output $result.Output
        $result
    }

    Write-Host "C-02 Room process restart test bat dau tren $Serial voi database fixture rieng."
    try {
        $seed = & $invokePhase 'seed' 'c02-restart-seed'
        Invoke-A06Adb -Serial $Serial -Arguments @('shell', 'am', 'force-stop', 'com.hoangtu.aistorystudio') -LogName 'c02-restart-force-stop' -Quiet | Out-Null
        Invoke-A06Adb -Serial $Serial -Arguments @(
            'shell', 'monkey', '-p', 'com.hoangtu.aistorystudio', '-c', 'android.intent.category.LAUNCHER', '1'
        ) -LogName 'c02-restart-launch' -Quiet | Out-Null
        $verify = & $invokePhase 'verify' 'c02-restart-verify'
        $cleanup = & $invokePhase 'cleanup' 'c02-restart-cleanup'
        Write-Host "C-02 process restart logs: $($seed.LogPath), $($verify.LogPath), $($cleanup.LogPath)"
    } catch {
        & $invokePhase 'cleanup' 'c02-restart-cleanup' | Out-Null
        throw
    }
}

function Assert-A06InstrumentationOutput {
    param([Parameter(Mandatory = $true)][string]$Output)

    $match = [regex]::Match($Output, 'OK\s*\(\s*(\d+)\s+tests?\s*\)')
    if (-not $match.Success) { throw 'AndroidJUnitRunner khong xac nhan dong OK (N tests); tu choi PASS.' }
    $count = [int]$match.Groups[1].Value
    $passed = [regex]::Matches($Output, 'INSTRUMENTATION_STATUS_CODE:\s*0(?:\s|$)').Count
    $failed = [regex]::Matches($Output, 'INSTRUMENTATION_STATUS_CODE:\s*-2(?:\s|$)').Count
    $skipped = [regex]::Matches($Output, 'INSTRUMENTATION_STATUS_CODE:\s*-3(?:\s|$)').Count
    Write-Host "AndroidJUnitRunner: tests=$count, passed-status=$passed, failures=$failed, skipped=$skipped"
    if ($count -le 0 -or $passed -ne $count -or $failed -ne 0 -or $skipped -ne 0 -or $Output -match 'FAILURES!!!|INSTRUMENTATION_FAILED') {
        throw 'AndroidJUnitRunner co 0 test, skip, failure hoac so trang thai khong khop.'
    }
}
