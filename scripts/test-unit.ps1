param([switch]$Offline)

. (Join-Path $PSScriptRoot 'Test-Common.ps1')
Initialize-A06Environment

$resultsDirectory = Join-Path $script:A06ProjectRoot 'app\build\test-results\testDebugUnitTest'
$arguments = @(':app:testDebugUnitTest', '--rerun-tasks', '--no-daemon', '--console=plain')
if ($Offline.IsPresent) { $arguments += '--offline' }
$run = Invoke-A06Gradle -Arguments $arguments -LogName 'unit'
$summary = Get-A06JUnitSummary -ResultsDirectory $resultsDirectory -StartedUtc $run.StartedUtc
if ($run.ExitCode -ne 0) { throw "Unit test Gradle exit code $($run.ExitCode). Log: $($run.LogPath)" }
Assert-A06TestCounts -Summary $summary -SuiteName 'Unit test'
