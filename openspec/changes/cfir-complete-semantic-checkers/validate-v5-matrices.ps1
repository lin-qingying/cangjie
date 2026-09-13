param(
    [switch]$GenerateMarkdown
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$matrixRoot = Join-Path $root 'matrices'
$repoRoot = (Resolve-Path (Join-Path $root '..\\..\\..')).Path

function Read-Matrix([string]$name) {
    $path = Join-Path $matrixRoot $name
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing matrix: $name" }
    return Get-Content -LiteralPath $path -Raw -Encoding utf8 | ConvertFrom-Json
}

function Require-Fields($entry, [string[]]$fields, [string]$label) {
    foreach ($field in $fields) {
        $value = $entry.PSObject.Properties[$field].Value
        if ($null -eq $value -or ([string]$value).Trim().Length -eq 0) {
            throw "${label}: missing field '$field'"
        }
    }
}

function Require-Schema($matrix, [string]$label) {
    if ($matrix.schemaVersion -ne 1) { throw "${label}: unsupported schemaVersion '$($matrix.schemaVersion)'" }
    if ($null -eq $matrix.entries) { throw "${label}: missing entries" }
}

function Require-StringArray($entry, [string]$field, [string]$label) {
    $value = $entry.PSObject.Properties[$field].Value
    if ($value -is [string] -or $null -eq $value) {
        throw "${label}: '$field' must be a JSON array of strings"
    }
    foreach ($item in @($value)) {
        if ($item -isnot [string] -or [string]::IsNullOrWhiteSpace($item)) {
            throw "${label}: '$field' contains a non-empty string"
        }
    }
}

function Require-Unique($values, [string]$label) {
    $duplicates = @($values | Group-Object | Where-Object Count -gt 1)
    if ($duplicates.Count -gt 0) { throw "${label}: duplicate values: $($duplicates.Name -join ', ')" }
}

$catalog = Read-Matrix 'annotation-catalog.json'
Require-Schema $catalog 'annotation-catalog'
$officialKinds = @('JAVA','CALLING_CONV','C','JAVA_MIRROR','JAVA_IMPL','JAVA_HAS_DEFAULT','OBJ_C_MIRROR','OBJ_C_IMPL','OBJ_C_INIT','OBJ_C_OPTIONAL','FOREIGN_NAME','FOREIGN_GETTER_NAME','FOREIGN_SETTER_NAME','ATTRIBUTE','NUMERIC_OVERFLOW','INTRINSIC','WHEN','FASTNATIVE','ANNOTATION','CONSTSAFE','DEPRECATED','FROZEN','ENSURE_PREPARED_TO_MOCK','NON_PRODUCT')
$catalogKinds = @($catalog.entries | Where-Object { $_.kind } | ForEach-Object kind | Sort-Object -Unique)
foreach ($kind in $officialKinds) { if ($kind -notin $catalogKinds) { throw "annotation-catalog: missing official kind $kind" } }
Require-Unique @($catalog.entries | ForEach-Object sourceSpelling) 'annotation-catalog source spellings'
foreach ($entry in $catalog.entries) { Require-Fields $entry @('sourceSpelling','grammar','target','visibility','phase','serialization','analysisApi','backendConsumer') "annotation-catalog/$($entry.sourceSpelling)" }
foreach ($state in @('CUSTOM','COMPILE_TIME_VISIBLE_CUSTOM','SYSTEM_MACRO','SPECIAL_EXPRESSION','PLATFORM_DERIVED','UNKNOWN')) { if ($state -notin @($catalog.stateIdentities)) { throw "annotation-catalog: missing state $state" } }

$ffi = Read-Matrix 'ffi-semantics-matrix.json'
Require-Schema $ffi 'ffi-semantics'
Require-Unique @($ffi.entries | ForEach-Object id) 'ffi ids'
foreach ($entry in $ffi.entries) {
    Require-Fields $entry @('id','construct','officialOwner','cfirOwner','requiredPaths') "ffi/$($entry.id)"
    Require-StringArray $entry 'requiredPaths' "ffi/$($entry.id)"
    Require-Unique @($entry.requiredPaths) "ffi/$($entry.id)/requiredPaths"
}
foreach ($id in @('foreign-marker','explicit-c','calling-conv','ctype-primitive','ctype-pointer-string','cfunc-wrapper','cfunc-signature','cfunc-value-invoke','c-struct','conversion','unsafe','inout','java-interop','objc-interop','cjmp','abi-resolution')) { if ($id -notin @($ffi.entries | ForEach-Object id)) { throw "ffi matrix: missing state $id" } }

$phase = Read-Matrix 'cfir-phase-state-matrix.json'
Require-Schema $phase 'phase-state'
Require-Unique @($phase.entries | ForEach-Object phase) 'phase names'
foreach ($entry in $phase.entries) { Require-Fields $entry @('phaseKind','phase','owner','input','output','minimumReadPhase','lazyTarget','reentrancy','failureTerminal','invalidation','duplicateDiagnostics') "phase/$($entry.phase)" }
foreach ($name in @('RAW_CFIR','IMPORTS','SUPER_TYPES','TYPES','STATUS','EXTENSIONS','IMPLICIT_TYPES','BODY_RESOLVE','CHECKER')) { if ($name -notin @($phase.entries | ForEach-Object phase)) { throw "phase matrix: missing phase $name" } }
$resolvePhaseNames = @($phase.entries | Where-Object phaseKind -eq 'resolve' | ForEach-Object phase)
$pipelinePhaseNames = @($phase.entries | Where-Object phaseKind -eq 'pipeline' | ForEach-Object phase)
if ($resolvePhaseNames.Count -ne 8 -or $pipelinePhaseNames -notcontains 'CHECKER') {
    throw 'phase matrix: resolve/pipeline phaseKind partition is incomplete'
}
$resolvePhaseSource = Join-Path $repoRoot 'cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirResolvePhase.kt'
if (-not (Test-Path -LiteralPath $resolvePhaseSource -PathType Leaf)) { throw "phase matrix: missing phase source $resolvePhaseSource" }
$resolvePhaseText = Get-Content -LiteralPath $resolvePhaseSource -Raw -Encoding utf8
foreach ($name in $resolvePhaseNames) {
    if ($resolvePhaseText -notmatch "(?m)^\\s*$name(?:\\(|,|$)") {
        throw "phase matrix: $name is not an actual CfirResolvePhase entry"
    }
}

$paths = Read-Matrix 'path-capability-matrix.json'
Require-Schema $paths 'path-capability'
Require-Unique @($paths.entries | ForEach-Object path) 'path names'
foreach ($entry in $paths.entries) { Require-Fields $entry @('path','reader','writer','capabilities','roundTrip','status') "path/$($entry.path)" }
foreach ($name in @('source','PSI','LightTree','CJO','stub','decompiled','Analysis API','CHIR','backend adapter')) { if ($name -notin @($paths.entries | ForEach-Object path)) { throw "path matrix: missing path $name" } }
$pathNames = @($paths.entries | ForEach-Object path)

$pc = Read-Matrix 'producer-consumer-matrix.json'
Require-Schema $pc 'producer-consumer'
Require-Unique @($pc.entries | ForEach-Object fact) 'producer-consumer facts'
foreach ($entry in $pc.entries) {
    Require-Fields $entry @('fact','producer','representation','consumers','forbiddenConsumers') "producer-consumer/$($entry.fact)"
    Require-StringArray $entry 'consumers' "producer-consumer/$($entry.fact)"
    Require-StringArray $entry 'forbiddenConsumers' "producer-consumer/$($entry.fact)"
    Require-Unique @($entry.consumers) "producer-consumer/$($entry.fact)/consumers"
    Require-Unique @($entry.forbiddenConsumers) "producer-consumer/$($entry.fact)/forbiddenConsumers"
}
foreach ($name in @('isForeign','explicit @C','ABI request','effective ABI','function type isCFunc','calling convention','ForeignName','Java/ObjC/CJMP metadata')) { if ($name -notin @($pc.entries | ForEach-Object fact)) { throw "producer-consumer matrix: missing fact $name" } }

foreach ($entry in $catalog.entries) {
    if ($entry.kind -and $entry.kind -notin $officialKinds) { throw "annotation-catalog/$($entry.sourceSpelling): unknown builtin kind $($entry.kind)" }
    if ($entry.phase -notin @($phase.entries | ForEach-Object phase)) { throw "annotation-catalog/$($entry.sourceSpelling): unknown phase $($entry.phase)" }
}
foreach ($entry in $ffi.entries) {
    foreach ($requiredPath in @($entry.requiredPaths)) {
        if ($requiredPath -notin $pathNames) { throw "ffi/$($entry.id): required path '$requiredPath' is not in path-capability matrix" }
    }
}

if ($GenerateMarkdown) {
    $out = Join-Path $root 'semantic-matrices.generated.md'
    $lines = [System.Collections.Generic.List[string]]::new()
    $lines.Add('# CFIR V5 semantic matrices')
    $lines.Add('')
    $lines.Add('Generated from the five JSON matrices by `validate-v5-matrices.ps1 -GenerateMarkdown`.')
    $lines.Add('')
    foreach ($pair in @(@('Annotation catalog',$catalog.entries), @('FFI semantics',$ffi.entries), @('Phase/state',$phase.entries), @('Path capability',$paths.entries), @('Producer/consumer',$pc.entries))) {
        $lines.Add("## $($pair[0])")
        $lines.Add('')
        $entries = @($pair[1])
        if ($entries.Count -gt 0) {
            $columns = @($entries[0].PSObject.Properties.Name | Where-Object { $_ -notin @('schemaVersion') })
            $lines.Add('| ' + ($columns -join ' | ') + ' |')
            $lines.Add('| ' + (($columns | ForEach-Object { '---' }) -join ' | ') + ' |')
            foreach ($entry in $entries) {
                $cells = foreach ($column in $columns) { ([string]$entry.PSObject.Properties[$column].Value).Replace('|','\\|').Replace("`r",' ').Replace("`n",' ') }
                $lines.Add('| ' + ($cells -join ' | ') + ' |')
            }
            $lines.Add('')
        }
    }
    [IO.File]::WriteAllText($out, ($lines -join "`n") + "`n", [Text.UTF8Encoding]::new($false))
}

$markdownPath = Join-Path $root 'semantic-matrices.generated.md'
if (-not (Test-Path -LiteralPath $markdownPath -PathType Leaf)) {
    throw "semantic-matrices.generated.md is missing; run with -GenerateMarkdown"
}
$markdown = Get-Content -LiteralPath $markdownPath -Raw -Encoding utf8
foreach ($section in @(
        @('Annotation catalog', @($catalog.entries)),
        @('FFI semantics', @($ffi.entries)),
        @('Phase/state', @($phase.entries)),
        @('Path capability', @($paths.entries)),
        @('Producer/consumer', @($pc.entries))
    )) {
    $sectionName = [regex]::Escape([string]$section[0])
    $sectionMatch = [regex]::Match($markdown, "(?ms)^## $sectionName\\s*$\\n(?<body>.*?)(?=^## |\\z)")
    if (-not $sectionMatch.Success) { throw "semantic-matrices.generated.md: missing section $($section[0])" }
    $tableRows = @($sectionMatch.Groups['body'].Value -split "`r?`n" | Where-Object { $_ -match '^\\| ' })
    $expectedRows = @($section[1]).Count + 2
    if ($tableRows.Count -ne $expectedRows) {
        throw "semantic-matrices.generated.md: section $($section[0]) has $($tableRows.Count - 2) entries; expected $(@($section[1]).Count)"
    }
}

Write-Output "V5 semantic matrices validated: annotation=$($catalog.entries.Count), ffi=$($ffi.entries.Count), phases=$($phase.entries.Count), paths=$($paths.entries.Count), producerConsumer=$($pc.entries.Count)"
