$ErrorActionPreference = "Stop"
$pattern = "cfir\analysis-tests\build\test-results\test\TEST-org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTTestGenerated*.xml"
$files = Get-ChildItem -Path $pattern

$totalTests = 0
$totalFailed = 0
$totalErrors = 0
$failingSuites = @()
$passingSuites = @()

foreach ($f in $files) {
    [xml]$xml = Get-Content $f.FullName
    $ts = $xml.testsuite
    $totalTests += [int]$ts.tests
    $totalFailed += [int]$ts.failures
    $totalErrors += [int]$ts.errors
    if ([int]$ts.failures -gt 0 -or [int]$ts.errors -gt 0) {
        $failingSuites += $ts
    } else {
        $passingSuites += $ts
    }
}

# --- Top-level summary ---
Write-Output "=== LLT 测试总体统计 ==="
Write-Output "总测试套件数: $($files.Count)"
Write-Output "通过套件数: $($passingSuites.Count)"
Write-Output "失败套件数: $($failingSuites.Count)"
Write-Output "总测试用例数: $totalTests"
Write-Output "总失败数: $totalFailed"
Write-Output "总错误数: $totalErrors"
Write-Output ""

# --- Failed suites breakdown ---
Write-Output "=== 失败套件明细 ==="
foreach ($ts in $failingSuites) {
    $displayName = ($ts.name -replace 'org\.cangnova\.cangjie\.cfir\.analysis\.tests\.CfirAnalysisLLTTestGenerated\$', '')
    Write-Output "$displayName | tests=$($ts.tests) failures=$($ts.failures) errors=$($ts.errors)"
}
