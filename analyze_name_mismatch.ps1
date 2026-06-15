$ErrorActionPreference = "Stop"
$pattern = "cfir\analysis-tests\build\test-results\test\TEST-org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisLLTTestGenerated*.xml"
$files = Get-ChildItem -Path $pattern

$results = @()

foreach ($f in $files) {
    [xml]$xml = Get-Content $f.FullName
    $nodes = $xml.SelectNodes("//failure")
    
    foreach ($node in $nodes) {
        $text = $node.InnerText
        
        $ematch = [regex]::Match($text, '=====.??.??.??======(.*?)=====.??.??.??======(.+)', 'Singleline')
        if (-not $ematch.Success) { continue }
        
        $expectedText = $ematch.Groups[1].Value
        $actualText = $ematch.Groups[2].Value
        
        # Extract markers
        $expMarkers = [regex]::Matches($expectedText, '<!([^!]+)!>') | ForEach-Object { $_.Groups[1].Value }
        $actMarkers = [regex]::Matches($actualText, '<!([^!]+)!>') | ForEach-Object { $_.Groups[1].Value }
        
        $maxCount = [Math]::Max($expMarkers.Count, $actMarkers.Count)
        for ($i = 0; $i -lt $maxCount; $i++) {
            $e = if ($i -lt $expMarkers.Count) { $expMarkers[$i] } else { "(none)" }
            $a = if ($i -lt $actMarkers.Count) { $actMarkers[$i] } else { "(none)" }
            
            if ($e -ne $a) {
                $suiteName = $f.Name -replace 'TEST-|\.xml$' -replace 'org\.cangnova\.cangjie\.cfir\.analysis\.tests\.CfirAnalysisLLTTestGenerated\$', ''
                
                $results += @(@{
                    Suite = $suiteName
                    Expected = $e
                    Actual = $a
                    File = $f.Name
                })
            }
        }
    }
}

Write-Output "=== DIAGNOSTIC NAME MISMATCH SUMMARY ==="
Write-Output "Total mismatches found: $($results.Count)"
Write-Output ""

$grouped = $results | Group-Object @{E={$_.Expected + " -> " + $_.Actual}} | Sort-Object Count -Descending

Write-Output "=== EXPECTED -> ACTUAL (by frequency) ==="
$grouped | ForEach-Object {
    Write-Output "[$($_.Count)x] $($_.Name)"
}

Write-Output ""
Write-Output "=== DETAIL BY EXPECTED NAME ==="
$byExpected = $results | Group-Object Expected | Sort-Object Count -Descending
$byExpected | ForEach-Object {
    $actuals = ($_.Group | Select-Object -ExpandProperty Actual -Unique) -join ", "
    Write-Output "$($_.Name) => [$actuals] ($($_.Count) times)"
}
