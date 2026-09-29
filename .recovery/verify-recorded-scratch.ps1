# 只读核对已完成 FileChange 中的原始 scratch；不执行历史命令，不写入文件。
$ErrorActionPreference = 'Stop'
$recoveryRoot = 'C:/Users/lin17/.codex/worktrees/recover-uncommitted/cangjie/'
$logBase = 'C:/Users/lin17/.codex/sessions/2026/09/'
$sources = @(
    @('A-main', '29/rollout-2026-09-29T07-07-40-01a0ea46-2571-7a51-ab29-168d65f086a0.jsonl'),
    @('A-sub1', '29/rollout-2026-09-29T07-08-55-01a0ea47-4725-75d0-a197-d714bd4edfa0.jsonl'),
    @('A-sub2', '29/rollout-2026-09-29T07-09-03-01a0ea47-677d-7192-9a2c-42e0ad39f53d.jsonl'),
    @('A-sub3', '29/rollout-2026-09-29T07-09-21-01a0ea47-ad30-7192-80bc-4bf5479628d2.jsonl'),
    @('B-main', '28/rollout-2026-09-28T15-41-00-01a0e6f5-c134-79c0-bcb8-6fc99b3adab5.jsonl'),
    @('B-sub1', '28/rollout-2026-09-28T15-42-05-01a0e6f6-bc5a-7020-8a2a-51ce4e495029.jsonl'),
    @('B-sub2', '28/rollout-2026-09-28T15-42-17-01a0e6f6-ee4a-7d20-97b6-41aa36424a83.jsonl'),
    @('B-sub3', '28/rollout-2026-09-28T15-42-26-01a0e6f7-105c-7922-b5ff-47ff21da81e3.jsonl')
)
$events = [System.Collections.Generic.List[object]]::new()
$sourceCounts = @{}
foreach ($source in $sources) {
    $sourceCounts[$source[0]] = 0
    $line = 0
    foreach ($recordLine in Get-Content -LiteralPath ($logBase + $source[1]) -Encoding UTF8) {
        $line++
        $r = $recordLine | ConvertFrom-Json -Depth 100
        if ($r.type -ne 'event_msg' -or $r.payload.item.type -ne 'FileChange' -or $r.payload.item.status -ne 'completed') { continue }
        foreach ($p in $r.payload.item.changes.PSObject.Properties) {
            $path = $p.Name.Replace('\', '/')
            # 排除本次恢复写入在同一个会话日志中追加的事件。
            if (-not $path.StartsWith('D:/code/intellij/cangjie/.workbuddy/')) { continue }
            if ($path.Split('/') -contains '..') { throw "Invalid relative path: $path" }
            $sourceCounts[$source[0]]++
            $events.Add([pscustomobject]@{
                session = $source[0]; log = $logBase + $source[1]; line = $line
                timestamp = $r.timestamp; id = $r.payload.item.id
                path = $path.Replace('D:/code/intellij/cangjie/', $recoveryRoot); change = $p.Value
            })
        }
    }
}
$expected = @{}
$history = @{}
foreach ($e in ($events | Sort-Object timestamp, session, line)) {
    $path = $e.path
    if (-not $history.ContainsKey($path)) { $history[$path] = @() }
    $history[$path] += [pscustomobject]@{ session=$e.session; line=$e.line; eventId=$e.id; timestamp=$e.timestamp }
    if ($e.change.type -eq 'add') {
        if ($expected.ContainsKey($path) -and $expected[$path] -cne $e.change.content) { throw "Conflicting add: $path" }
        $expected[$path] = [string]$e.change.content
        continue
    }
    if ($e.change.type -ne 'update' -or -not $expected.ContainsKey($path)) { throw "No exact base: $path" }
    $sourceLines = [System.Collections.Generic.List[string]]::new()
    $sourceLines.AddRange([string[]]$expected[$path].Split([char]10))
    $diff = [string[]]$e.change.unified_diff.Split([char]10)
    $delta = 0
    $i = 0
    while ($i -lt $diff.Length) {
        if ($diff[$i] -eq '') { $i++; continue }
        $header = [regex]::Match($diff[$i], '^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@')
        if (-not $header.Success) { throw "Invalid diff at $($e.session):$($e.line)" }
        $j = $i + 1
        $before = [System.Collections.Generic.List[string]]::new()
        $after = [System.Collections.Generic.List[string]]::new()
        while ($j -lt $diff.Length -and -not $diff[$j].StartsWith('@@')) {
            $s = $diff[$j]
            if ($s.StartsWith(' ') -or $s.StartsWith('-')) { $before.Add($s.Substring(1)) }
            if ($s.StartsWith(' ') -or $s.StartsWith('+')) { $after.Add($s.Substring(1)) }
            $j++
        }
        $at = [int]$header.Groups[1].Value - 1 + $delta
        if (($sourceLines.GetRange($at, $before.Count) -join [char]10) -cne ($before -join [char]10)) {
            throw "Diff context mismatch: $path at $($e.session):$($e.line)"
        }
        $sourceLines.RemoveRange($at, $before.Count)
        $sourceLines.InsertRange($at, $after)
        $delta += $after.Count - $before.Count
        $i = $j
    }
    $expected[$path] = $sourceLines -join [char]10
}
$files = @(foreach ($path in ($expected.Keys | Sort-Object)) {
    $hash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($expected[$path])))
    $actual = if (Test-Path -LiteralPath $path) { (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash } else { $null }
    [pscustomobject]@{ path=$path; expectedSHA256=$hash; actualSHA256=$actual; matches=($hash -ceq $actual); evidence=$history[$path] }
})
[pscustomobject]@{
    recoveryRoot=$recoveryRoot; sourceLogs=@($sources | ForEach-Object { [pscustomobject]@{session=$_[0]; path=$logBase+$_[1]} })
    originalFileChangeCounts=$sourceCounts; files=$files
    verifiedCount=@($files | Where-Object matches).Count; mismatchCount=@($files | Where-Object { -not $_.matches }).Count
    excluded='Recovery-target FileChange events, unrecorded compiler output bytes, and formal source paths.'
} | ConvertTo-Json -Depth 12 -Compress
