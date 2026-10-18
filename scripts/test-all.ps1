param([string]$DeviceSerial)

. (Join-Path $PSScriptRoot 'Test-Common.ps1')
Initialize-A06Environment

& (Join-Path $PSScriptRoot 'test-harness.ps1')
if (-not $?) { throw 'Test harness zero/skip guard failed.' }

$resultsDirectory = Join-Path $script:A06ProjectRoot 'app\build\test-results\testDebugUnitTest'
$run = Invoke-A06Gradle -Arguments @(':app:assembleDebug', ':app:testDebugUnitTest', ':app:lintDebug', '--rerun-tasks', '--no-daemon', '--console=plain') -LogName 'all-build-unit-lint'
$summary = Get-A06JUnitSummary -ResultsDirectory $resultsDirectory -StartedUtc $run.StartedUtc
if ($run.ExitCode -ne 0) { throw "Build/unit/lint Gradle exit code $($run.ExitCode). Log: $($run.LogPath)" }
Assert-A06TestCounts -Summary $summary -SuiteName 'Unit test'

$instrumentationScript = Join-Path $PSScriptRoot 'test-instrumentation.ps1'
if ([string]::IsNullOrWhiteSpace($DeviceSerial)) {
    & $instrumentationScript
} else {
    & $instrumentationScript -DeviceSerial $DeviceSerial
}
if (-not $?) { throw 'Instrumentation script failed.' }

Write-Host 'A-06 build/unit/lint/instrumentation pipeline: PASS'
