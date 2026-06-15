$ErrorActionPreference = "Stop"

# Pick a few representative failing test XMLs and show their detailed diff
$files = @(
    "cfir\analysis-tests\build\test-results\test\TEST-org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTTestGenerated$Assign.xml",
    "cfir\analysis-tests\build\test-results\test\TEST-org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTTestGenerated$Binary.xml",
    "cfir\analysis-tests\build\test-results\test\TEST-org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTTestGenerated$Blockexpr.xml",
    "cfir\analysis-tests\build\test-results\test\TEST-org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTTestGenerated$Call.xml",
    "cfir\analysis-tests\build\test-results\test\TEST-org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTTestGenerated$ErrMsgs.xml",
    "cfir\analysis-tests\build\test-results\test\TEST-org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTTestGenerated$InitializationCheck.xml",
    "cfir\analysis-tests\build\test-results\test\TEST-org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTTestGenerated$Varray.xml"
)

foreach ($f in $files) {
    if (-not (Test-Path $f)) { continue }
    [xml]$xml = Get-Content $f
    $testCases = $xml.SelectNodes("//testcase[failure]")
    
    Write-Output "=== $f ==="
    Write-Output "  Total failing: $($testCases.Count)"
    
    $count = 0
    foreach ($tc in $testCases) {
        if ($count -ge 3) { break }
        $count++
        $failureText = $tc.failure.InnerText
        
        # Extract the diff portion
        $expectedMatch = [regex]::Match($failureText, '=====预期======(.*?)=====得到======(.+)', 'Singleline')
        if ($expectedMatch.Success) {
            $expected = $expectedMatch.Groups[1].Value.Trim()
            $actual = $expectedMatch.Groups[2].Value.Trim()
            
            # Show only the lines that differ
            $eLines = $expected -split "`n"
            $aLines = $actual -split "`n"
            
            Write-Output "`n[$($tc.classname) > $($tc.name)]"
            $maxLines = [Math]::Min(10, [Math]::Max($eLines.Count, $aLines.Count))
            for ($i = 0; $i -lt $maxLines; $i++) {
                $e = if ($i -lt $eLines.Count) { $eLines[$i] } else { "" }
                $a = if ($i -lt $aLines.Count) { $aLines[$i] } else { "" }
                if ($e -ne $a) {
                    Write-Output "  -$e"
                    Write-Output "  +$a"
                }
            }
        } else {
            Write-Output "`n[$($tc.classname) > $($tc.name)]"
            Write-Output "  (no structured diff, raw error)"
            Write-Output "  $($tc.failure.message)"
        }
    }
    Write-Output ""
}
