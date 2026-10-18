param(
    [string]$DeviceSerial,
    [switch]$UseGradleRunner
)

. (Join-Path $PSScriptRoot 'Test-Common.ps1')
Initialize-A06Environment
$device = Select-A06Emulator -DeviceSerial $DeviceSerial
$env:ANDROID_SERIAL = $device.Serial
Write-Host "Target emulator: $($device.Serial), API $($device.Api), online devices=$($device.OnlineCount)"

if ($UseGradleRunner.IsPresent) {
    if ($device.OnlineCount -ne 1) {
        throw 'Gradle instrumentation co the chay tren tat ca device online va go APK sau test; chi cho phep khi chi co mot emulator online. Dung runner truc tiep cho nhieu device.'
    }
    Write-Warning 'Gradle connectedDebugAndroidTest se go app APK va test APK khoi emulator khi ket thuc; chi dung tren AVD co the cai lai va khong can giu app data.'
    $resultsDirectory = Join-Path $script:A06ProjectRoot 'app\build\outputs\androidTest-results\connected\debug'
    $run = Invoke-A06Gradle -Arguments @(':app:connectedDebugAndroidTest', '--rerun-tasks', '--no-daemon', '--console=plain', '--info') -LogName 'instrumentation-gradle'
    $summary = Get-A06JUnitSummary -ResultsDirectory $resultsDirectory -StartedUtc $run.StartedUtc

    if ($summary.Tests -gt 0 -and ($summary.Failures -gt 0 -or $summary.Errors -gt 0 -or $summary.Skipped -gt 0)) {
        Assert-A06TestCounts -Summary $summary -SuiteName 'Instrumentation Gradle'
    }
    if ($run.ExitCode -eq 0 -and $summary.Tests -gt 0 -and $summary.Failures -eq 0 -and $summary.Errors -eq 0 -and $summary.Skipped -eq 0) {
        Assert-A06TestCounts -Summary $summary -SuiteName 'Instrumentation Gradle'
        Write-Host "Gradle instrumentation log: $($run.LogPath)"
        return
    }

    Write-Warning "Gradle runner khong tao report co test duong tinh (exit=$($run.ExitCode), reports=$($summary.ReportCount), tests=$($summary.Tests)); chuyen sang AndroidJUnitRunner truc tiep chi tren $($device.Serial)."
}

Invoke-A06DirectInstrumentation -Serial $device.Serial
