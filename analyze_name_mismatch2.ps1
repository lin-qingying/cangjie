$ErrorActionPreference = "Stop"
$pattern = "cfir\analysis-tests\build\test-results\test\TEST-org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTTestGenerated*.xml"
$files = Get-ChildItem -Path $pattern

$realMismatches = @{}

foreach ($f in $files) {
    [xml]$xml = Get-Content $f.FullName
    $nodes = $xml.SelectNodes("//failure")
    
    foreach ($node in $nodes) {
        $text = $node.InnerText
        
        $ematch = [regex]::Match($text, '=====.+======(.*?)=====.+======(.+)', 'Singleline')
        if (-not $ematch.Success) { continue }
        
        $expectedText = $ematch.Groups[1].Value
        $actualText = $ematch.Groups[2].Value
        
        $expMarkers = [regex]::Matches($expectedText, '<!([^!]+)!>') | ForEach-Object { $_.Groups[1].Value }
        $actMarkers = [regex]::Matches($actualText, '<!([^!]+)!>') | ForEach-Object { $_.Groups[1].Value }
        
        $minCount = [Math]::Min($expMarkers.Count, $actMarkers.Count)
        for ($i = 0; $i -lt $minCount; $i++) {
            $e = $expMarkers[$i]
            $a = $actMarkers[$i]
            
            # Filter: both are real diagnostic names (not single-char, not "none")
            $isRealName = { param($n) $n.Length -gt 2 -and $n -notmatch '^\W+$' }
            
            if ($e -ne $a -and $e.Length -gt 2 -and $a.Length -gt 2 -and $e -notmatch '^\W+$' -and $a -notmatch '^\W+$') {
                $key = "$e -> $a"
                if (-not $realMismatches.ContainsKey($key)) { $realMismatches[$key] = 0 }
                $realMismatches[$key]++
            }
        }
        
        # Extra markers in actual (not in expected)
        if ($actMarkers.Count -gt $expMarkers.Count) {
            for ($i = $expMarkers.Count; $i -lt $actMarkers.Count; $i++) {
                $a = $actMarkers[$i]
                if ($a.Length -gt 2 -and $a -notmatch '^\W+$') {
                    $key = "(missing in expected) -> $a"
                    if (-not $realMismatches.ContainsKey($key)) { $realMismatches[$key] = 0 }
                    $realMismatches[$key]++
                }
            }
        }
        
        # Missing markers in actual (expected has, actual doesn't)
        if ($expMarkers.Count -gt $actMarkers.Count) {
            for ($i = $actMarkers.Count; $i -lt $expMarkers.Count; $i++) {
                $e = $expMarkers[$i]
                if ($e.Length -gt 2 -and $e -notmatch '^\W+$') {
                    $key = "$e -> (missing in actual)"
                    if (-not $realMismatches.ContainsKey($key)) { $realMismatches[$key] = 0 }
                    $realMismatches[$key]++
                }
            }
        }
    }
}

Write-Output "=== REAL DIAGNOSTIC NAME MISMATCHES ==="
Write-Output ""

$sorted = $realMismatches.GetEnumerator() | Sort-Object Value -Descending

# Category 1: Different name at same position
Write-Output "--- SAME POSITION, DIFFERENT NAME ---"
$sorted | Where-Object { $_.Key -notmatch '\(missing' } | ForEach-Object {
    Write-Output "[$($_.Value)x] $($_.Key)"
}

Write-Output ""
Write-Output "--- MISSING FROM ACTUAL (test expected, compiler didn't emit at that position) ---"
$sorted | Where-Object { $_.Key -match '\(missing in actual\)' } | ForEach-Object {
    Write-Output "[$($_.Value)x] $($_.Key)"
}

Write-Output ""
Write-Output "--- EXTRA IN ACTUAL (compiler emitted, test didn't expect at that position) ---"
$sorted | Where-Object { $_.Key -match '\(missing in expected\)' } | ForEach-Object {
    Write-Output "[$($_.Value)x] $($_.Key)"
}
