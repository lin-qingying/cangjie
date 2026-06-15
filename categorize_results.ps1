$ErrorActionPreference = "Stop"
$pattern = "cfir\analysis-tests\build\test-results\test\TEST-org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTTestGenerated*.xml"
$files = Get-ChildItem -Path $pattern

$categoryMap = @{
    "diagnostic_name_mismatch" = 0
    "missing_diagnostic" = 0
    "extra_diagnostic" = 0
    "compiler_crash" = 0
    "position_shift" = 0
    "parser_vs_semantic" = 0
    "other" = 0
}
$details = @()

foreach ($f in $files) {
    [xml]$xml = Get-Content $f.FullName
    
    # Get the root suite name
    $rootSuite = $xml.SelectSingleNode("//testsuite")
    if (-not $rootSuite) { continue }
    
    $testCases = $xml.SelectNodes("//testcase[failure]")
    
    foreach ($tc in $testCases) {
        $failureMsg = $tc.failure.message
        $failureText = $tc.failure.InnerText
        $className = $tc.classname
        $testName = $tc.name
        
        # Determine category
        $cat = "other"
        if ($failureText -match "expected.*!=.*actual|Different content|Comparison Failure|expectedString") {
            $cat = "diagnostic_name_mismatch"
        }
        if ($failureText -match "CangJieIllegalArgumentException|IndexOutOfBoundsException|NullPointerException|stack trace|Exception in") {
            $cat = "compiler_crash"
        }
        if ($failureText -match "expected no diagnostic|unexpected diagnostic|extra diagnostic") {
            $cat = "extra_diagnostic"
        }
        if ($failureText -match "expected diagnostic was not|missing diagnostic|expected .*? but no diagnostic") {
            $cat = "missing_diagnostic"
        }
        if ($failureText -match "position.*?different|offset.*?mismatch|wrong line") {
            $cat = "position_shift"
        }
        if ($failureText -match "parse_|lex_") {
            $cat = "parser_vs_semantic"
        }
        
        $categoryMap[$cat]++
        
        # Store first 3 details per category
        if ($details.Count -lt 200) {
            $details += @{
                category = $cat
                suite = $rootSuite.name
                test = $testName
                msg = ($failureMsg -replace '\s+', ' ').Substring(0, [Math]::Min(120, ($failureMsg -replace '\s+', ' ').Length))
                text = $failureText.Substring(0, [Math]::Min(500, $failureText.Length))
            }
        }
    }
}

Write-Output "=== 失败分类统计 ==="
$categoryMap.GetEnumerator() | Sort-Object Value -Descending | ForEach-Object {
    Write-Output "$($_.Key): $($_.Value)"
}

Write-Output "`n=== 各分类典型示例 ==="
$shown = @{}
foreach ($d in $details) {
    if (-not $shown.ContainsKey($d.category) -or $shown[$d.category] -lt 5) {
        Write-Output ""
        Write-Output "[$($d.category)] $($d.suite)$ > $($d.test)"
        Write-Output "  msg: $($d.msg)"
        # Show first 3 lines of diff
        $lines = $d.text -split "`n"
        for ($i = 0; $i -lt [Math]::Min(6, $lines.Count); $i++) {
            if ($lines[$i].Trim().Length -gt 0) {
                Write-Output "  $($lines[$i].Trim())"
            }
        }
        if (-not $shown.ContainsKey($d.category)) { $shown[$d.category] = 1 } else { $shown[$d.category]++ }
    }
}
