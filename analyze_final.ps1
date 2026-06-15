$ErrorActionPreference = "Stop"
$pattern = "cfir\analysis-tests\build\test-results\test\TEST-org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTTestGenerated*.xml"
$files = Get-ChildItem -Path $pattern

$totalTests = 0
$totalFailed = 0
$zeroFailSuites = 0
$nonZeroFailSuites = 0
$suiteStats = @()

foreach ($f in $files) {
    [xml]$xml = Get-Content $f.FullName
    $ts = $xml.testsuite
    $t = [int]$ts.tests
    $fail = [int]$ts.failures
    $totalTests += $t
    $totalFailed += $fail
    
    $displayName = ($ts.name -replace 'org\.cangnova\.cangjie\.cfir\.analysis\.tests\.CfirAnalysisLLTTestGenerated\$', '')
    
    if ($fail -eq 0) {
        $zeroFailSuites++
    } else {
        $nonZeroFailSuites++
        $suiteStats += @{ Name = $displayName; Tests = $t; Failures = $fail }
    }
}

Write-Output "=== LLT TEST SUMMARY ==="
Write-Output "Total suites: " + $files.Count
Write-Output "Passing: " + $zeroFailSuites
Write-Output "Failing: " + $nonZeroFailSuites
Write-Output ""
Write-Output "Total tests: " + $totalTests
Write-Output "Total failures: " + $totalFailed

# Check sema_ remaining
$remainingSema = Get-ChildItem -Path "cfir\analysis-tests\testData\llt" -Recurse -Filter "*.cj" | Select-String -Pattern '<!sema_' -SimpleMatch -List
if ($remainingSema.Count -eq 0) {
    Write-Output "sema_ remaining: 0 (all clean)"
} else {
    Write-Output "sema_ remaining: " + $remainingSema.Count + " files"
}
Write-Output ""

# Top failing suites
$sorted = $suiteStats | Sort-Object Failures -Descending
Write-Output "=== TOP FAILING SUITES (30) ==="
$sorted | Select-Object -First 30 | ForEach-Object {
    Write-Output ($_.Name + " -> " + $_.Failures + " fails / " + $_.Tests + " tests")
}
