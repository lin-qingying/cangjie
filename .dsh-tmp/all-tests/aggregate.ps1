
param(
  [string]$Root = 'D:\code\intellij\cangjie',
  [string]$OutJson = 'D:\code\intellij\cangjie\.dsh-tmp\all-tests\summary.json',
  [string]$RunStart = '2026-11-01 17:58:00'
)
$start = Get-Date $RunStart
$skipTop = @('.claude','.git','external','intellij-ide','deveco','build','target','.dsh-tmp','.gradle','.scratch','tmp')
$rows = @()
$dirs = Get-ChildItem -Path $Root -Directory -Recurse -Filter 'test-results' -ErrorAction SilentlyContinue | Where-Object { $_.FullName -match '\\build\\test-results$' }
foreach ($rd in $dirs) {
  $moduleDir = Split-Path (Split-Path $rd.FullName -Parent) -Parent
  $rel = $moduleDir.Substring($Root.Length).Trim('\')
  if ([string]::IsNullOrEmpty($rel)) { continue }
  if ($skipTop -contains (($rel -split '\\')[0])) { continue }
  foreach ($taskDir in (Get-ChildItem $rd.FullName -Directory)) {
    if ($taskDir.Name -like 'test-stale-*') { continue }
    $xmls = Get-ChildItem $taskDir.FullName -Filter '*.xml' -File -ErrorAction SilentlyContinue
    if (-not $xmls) { continue }
    $tests=0;$failures=0;$errors=0;$skipped=0;$time=0.0;$stale=0
    $failed = New-Object System.Collections.ArrayList
    foreach ($x in $xmls) {
      if ($x.LastWriteTime -lt $start) { $stale++ }
      try { [xml]$doc = Get-Content $x.FullName -Raw } catch { continue }
      $s = $doc.testsuite
      if ($null -eq $s) { continue }
      $tests += [int]$s.tests; $failures += [int]$s.failures; $errors += [int]$s.errors; $skipped += [int]$s.skipped
      $time += [double]$s.time
      foreach ($c in $s.testcase) {
        $bad = $c.failure; $kind='failure'
        if (-not $bad) { $bad = $c.error; $kind='error' }
        if ($bad) {
          $msg = ''
          if ($bad -is [System.Xml.XmlElement]) { $msg = "$($bad.message)" } elseif ($bad -is [array]) { $msg = "$($bad[0].message)" }
          $flat = ($msg -replace '\s+',' ').Trim()
          if ($flat.Length -gt 500) { $flat = $flat.Substring(0,500) }
          [void]$failed.Add([pscustomobject]@{ class="$($c.classname)"; name="$($c.name)"; type=$kind; message=$flat })
        }
      }
    }
    $rows += [pscustomobject]@{ module=$rel; task=$taskDir.Name; tests=$tests; failures=$failures; errors=$errors; skipped=$skipped; timeSec=[Math]::Round($time,1); staleXml=$stale; failed=$failed }
  }
}
$tot = [pscustomobject]@{
  modules = ($rows | Select-Object -ExpandProperty module -Unique).Count
  tasks = $rows.Count
  tests = ($rows | Measure-Object tests -Sum).Sum
  failures = ($rows | Measure-Object failures -Sum).Sum
  errors = ($rows | Measure-Object errors -Sum).Sum
  skipped = ($rows | Measure-Object skipped -Sum).Sum
  timeSec = [Math]::Round((($rows | Measure-Object timeSec -Sum).Sum),1)
  staleModules = ($rows | Where-Object { $_.staleXml -gt 0 } | Select-Object -ExpandProperty module -Unique)
}
[pscustomobject]@{ total=$tot; rows=$rows } | ConvertTo-Json -Depth 7 | Set-Content -Path $OutJson -Encoding UTF8
Write-Output ("MODULES={0} TASKS={1} TESTS={2} FAIL={3} ERR={4} SKIP={5} TIME={6}s" -f $tot.modules,$tot.tasks,$tot.tests,$tot.failures,$tot.errors,$tot.skipped,$tot.timeSec)
Write-Output ("stale modules: " + ($tot.staleModules -join ', '))
$rows | Sort-Object { $_.failures + $_.errors } -Descending | Select-Object -First 20 module,tests,failures,timeSec | Format-Table -AutoSize | Out-String -Width 200
