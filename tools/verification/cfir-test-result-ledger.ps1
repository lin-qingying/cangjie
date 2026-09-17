[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$SnapshotName,
    [string]$CompareTo
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$resultsPath = Join-Path $repositoryRoot 'cfir/analysis-tests/build/test-results/test'
$archiveRoot = Join-Path $repositoryRoot 'cfir/analysis-tests/build/ffi-annotation-verification'
$snapshotPath = [IO.Path]::GetFullPath((Join-Path $archiveRoot $SnapshotName))
if (-not $snapshotPath.StartsWith($archiveRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Snapshot must stay in the verification directory.'
}
if (Test-Path -LiteralPath $snapshotPath) { throw "Snapshot already exists: $snapshotPath" }
[IO.Directory]::CreateDirectory($snapshotPath) | Out-Null

# 在后续 Gradle 覆盖 test-results 前保存原始 XML 和可重算的 testcase-key 台账。
$files = @(Get-ChildItem -LiteralPath $resultsPath -Filter '*.xml')
$rows = [Collections.Generic.List[object]]::new()
$zip = [IO.Compression.ZipFile]::Open((Join-Path $snapshotPath 'results.zip'), [IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($file in $files) {
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $file.FullName, $file.Name) | Out-Null
        [xml]$xml = Get-Content -LiteralPath $file.FullName -Encoding UTF8 -Raw
        foreach ($case in $xml.testsuite.testcase) {
            $failure = $case.SelectSingleNode('failure|error')
            $message = ([string]$failure.message).Replace("`r`n", "`n")
            $rows.Add([pscustomobject]@{
                key = "$($case.classname)#$($case.name)"
                status = if ($null -ne $failure) { 'failed' } elseif ($null -ne $case.SelectSingleNode('skipped')) { 'skipped' } else { 'passed' }
                message = $message
                hash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($message)))
            })
        }
    }
} finally { $zip.Dispose() }

$summary = [ordered]@{
    snapshot = $snapshotPath
    xmlFiles = $files.Count
    records = $rows.Count
    passed = @($rows | Where-Object status -eq 'passed').Count
    failed = @($rows | Where-Object status -eq 'failed').Count
    skipped = @($rows | Where-Object status -eq 'skipped').Count
}
$utf8 = [Text.UTF8Encoding]::new($false)
[IO.File]::WriteAllText((Join-Path $snapshotPath 'cases.json'), (ConvertTo-Json -InputObject @($rows.ToArray()) -Depth 5), $utf8)

if ($CompareTo) {
    $previous = Get-Content -LiteralPath (Join-Path $CompareTo 'cases.json') -Encoding UTF8 -Raw | ConvertFrom-Json
    $old = @{}
    foreach ($case in $previous) { $old[$case.key] = $case }
    $changes = [ordered]@{ fixed = @(); regressed = @(); new = @(); removed = @(); unchangedFailures = @(); changedFailures = @() }
    foreach ($case in $rows) {
        $before = $old[$case.key]
        if (-not $before) { $changes.new += $case; continue }
        if ($before.status -eq 'failed' -and $case.status -eq 'passed') { $changes.fixed += $case.key }
        if ($before.status -eq 'passed' -and $case.status -eq 'failed') { $changes.regressed += $case.key }
        if ($before.status -eq 'failed' -and $case.status -eq 'failed') {
            if ($before.hash -eq $case.hash) { $changes.unchangedFailures += $case.key } else { $changes.changedFailures += $case.key }
        }
        $old.Remove($case.key)
    }
    $changes.removed = @($old.Keys | Sort-Object)
    [IO.File]::WriteAllText((Join-Path $snapshotPath 'comparison.json'), ($changes | ConvertTo-Json -Depth 5), $utf8)
    foreach ($name in $changes.Keys) { $summary[$name] = @($changes[$name]).Count }
}
[IO.File]::WriteAllText((Join-Path $snapshotPath 'summary.json'), ($summary | ConvertTo-Json -Depth 4), $utf8)
$summary | ConvertTo-Json -Compress
