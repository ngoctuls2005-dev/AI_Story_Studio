. (Join-Path $PSScriptRoot 'Test-Common.ps1')

Assert-A06TestCounts -Summary ([pscustomobject]@{ ReportCount = 1; Tests = 1; Failures = 0; Errors = 0; Skipped = 0; ReportPaths = @('fixture.xml') }) -SuiteName 'Harness accepted fixture' | Out-Null

$rejected = 0
foreach ($invalid in @(
    [pscustomobject]@{ ReportCount = 1; Tests = 0; Failures = 0; Errors = 0; Skipped = 0; ReportPaths = @('zero.xml') },
    [pscustomobject]@{ ReportCount = 1; Tests = 2; Failures = 0; Errors = 0; Skipped = 1; ReportPaths = @('skipped.xml') }
)) {
    try {
        Assert-A06TestCounts -Summary $invalid -SuiteName 'Harness rejection fixture' | Out-Null
    } catch {
        $rejected++
    }
}
if ($rejected -ne 2) { throw "Harness chi tu choi $rejected/2 fixture khong hop le." }

$runnerAccepted = "INSTRUMENTATION_STATUS_CODE: 0`nOK (1 test)"
Assert-A06InstrumentationOutput -Output $runnerAccepted | Out-Null
$runnerRejected = 0
foreach ($output in @(
    "INSTRUMENTATION_STATUS_CODE: 0`nOK (0 tests)",
    "INSTRUMENTATION_STATUS_CODE: -3`nOK (1 test)"
)) {
    try {
        Assert-A06InstrumentationOutput -Output $output | Out-Null
    } catch {
        $runnerRejected++
    }
}
if ($runnerRejected -ne 2) { throw "Runner guard chi tu choi $runnerRejected/2 fixture khong hop le." }
Write-Host 'Harness guard: PASS — runner chap nhan 1/1 va tu choi 0 test, skipped test.'
