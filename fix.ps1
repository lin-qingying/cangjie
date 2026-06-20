$filePath = "d:\code\intellij\cangjie\cfir\analysis-tests\testFixtures\org\cangnova\cangjie\cfir\analysis\tests\TestGeneratorForCfirAnalysisTests.kt"
$content = Get-Content $filePath -Raw
$lines = $content -split "`r`n"
Write-Host "Total lines: $($lines.Length)"
Write-Host "Line 217: $($lines[216])"
Write-Host "Line 218: $($lines[217])"
Write-Host "Line 219: $($lines[218])"

# Find the line with "// 根包名 = basePackage + testDataRootName"
$idx = -1
for ($i = 0; $i -lt $lines.Length; $i++) {
    if ($lines[$i] -match '^\s*// 根包名 = basePackage \+ testDataRootName$') {
        $idx = $i
        break
    }
}
Write-Host "Found at index: $idx"

if ($idx -ge 0) {
    # Insert new lines before this line
    $newLines = @()
    for ($i = 0; $i -lt $idx; $i++) {
        $newLines += $lines[$i]
    }
    $newLines += "        // 入口点包名 = basePackage（不含 testDataRootName）"
    $newLines += "        val entryPointPackageName = basePackage"
    $newLines += ""
    $newLines += $lines[$idx]  # Keep the original comment line
    $newLines += $lines[$idx + 1]  # Keep the val rootPackageName line
    for ($i = $idx + 2; $i -lt $lines.Length; $i++) {
        $newLines += $lines[$i]
    }
    $newLines -join "`r`n" | Set-Content $filePath -NoNewline
    Write-Host "File updated"
} else {
    Write-Host "Pattern not found"
}