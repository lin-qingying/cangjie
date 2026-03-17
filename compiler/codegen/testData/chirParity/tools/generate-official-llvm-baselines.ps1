param(
    [string]$SdkPath = "C:\Users\lin17\.cangjie\sdks\cangjie-1.0.0",
    [string]$FixtureSubdir = ".",
    [ValidateSet("O0", "O1", "O2", "Os", "Oz")]
    [string]$OptimizationLevel = "O0",
    [string]$OutputRootDirName = "official-generated-llvm"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-RepoRoot {
    param([string]$Start)
    $cursor = Resolve-Path -LiteralPath $Start
    while ($true) {
        if (Test-Path (Join-Path $cursor "settings.gradle.kts")) {
            return $cursor
        }
        $parent = Split-Path -Parent $cursor
        if ($parent -eq $cursor) {
            throw "Cannot locate repo root from $Start"
        }
        $cursor = $parent
    }
}

function Get-RelativePathCompat {
    param(
        [Parameter(Mandatory = $true)][string]$BasePath,
        [Parameter(Mandatory = $true)][string]$TargetPath
    )
    $baseFull = [System.IO.Path]::GetFullPath($BasePath)
    $targetFull = [System.IO.Path]::GetFullPath($TargetPath)
    if (-not $baseFull.EndsWith([System.IO.Path]::DirectorySeparatorChar)) {
        $baseFull = $baseFull + [System.IO.Path]::DirectorySeparatorChar
    }
    $baseUri = New-Object System.Uri($baseFull)
    $targetUri = New-Object System.Uri($targetFull)
    return [System.Uri]::UnescapeDataString(
        $baseUri.MakeRelativeUri($targetUri).ToString().Replace('/', [System.IO.Path]::DirectorySeparatorChar)
    )
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Get-RepoRoot -Start $scriptDir
$fixtureRoot = Join-Path $repoRoot "compiler\codegen\testData\chirParity"
$outputRoot = Join-Path $repoRoot ("compiler\codegen\testData\chirParity\" + $OutputRootDirName)
$fixtureSearchRoot = Resolve-Path -LiteralPath (Join-Path $fixtureRoot $FixtureSubdir)
$sourceRoot = Join-Path $scriptDir "official-sources"
$tmpRoot = Join-Path $env:TEMP ("cj-official-baseline-" + [Guid]::NewGuid().ToString())

if (!(Test-Path $SdkPath)) {
    throw "SDK path not found: $SdkPath"
}

$envSetup = Join-Path $SdkPath "envsetup.ps1"
if (!(Test-Path $envSetup)) {
    throw "envsetup.ps1 not found: $envSetup"
}

. $envSetup

$cjcPath = Join-Path $SdkPath "bin\cjc.exe"
$llvmDisPath = Join-Path $SdkPath "third_party\llvm\bin\llvm-dis.exe"
if (!(Test-Path $cjcPath)) {
    throw "cjc not found: $cjcPath"
}
if (!(Test-Path $llvmDisPath)) {
    throw "llvm-dis not found: $llvmDisPath"
}

New-Item -ItemType Directory -Path $tmpRoot -Force | Out-Null

$generated = 0
$skipped = New-Object System.Collections.Generic.List[string]

try {
    $fixtures = Get-ChildItem -Path $fixtureSearchRoot -Filter "*.chir.json" -File -Recurse | Sort-Object FullName
    foreach ($fixture in $fixtures) {
        $relativeFixture = Get-RelativePathCompat -BasePath $fixtureRoot -TargetPath $fixture.FullName
        $relativeDir = Split-Path -Parent $relativeFixture
        $targetDir = Join-Path $outputRoot $relativeDir
        New-Item -ItemType Directory -Path $targetDir -Force | Out-Null

        $sample = [System.IO.Path]::GetFileNameWithoutExtension([System.IO.Path]::GetFileNameWithoutExtension($fixture.Name))
        $payload = Get-Content -LiteralPath $fixture.FullName -Raw | ConvertFrom-Json
        $relativeSource = $relativeFixture -replace '\.chir\.json$', '.cj'

        $scenario = $null
        if ($payload.PSObject.Properties.Name -contains "scenario") {
            $scenario = [string]$payload.scenario
        }
        if ($null -ne $scenario -and $scenario -ne "" -and $scenario -ne "return") {
            $scenario = $scenario.Trim()
        }

        $sourceFile = $null
        $candidateSources = @(
            ($fixture.FullName -replace '\.chir\.json$', '.cj'),
            (Join-Path $sourceRoot $relativeSource)
        )

        foreach ($candidate in $candidateSources) {
            if (Test-Path -LiteralPath $candidate) {
                $sourceFile = (Resolve-Path -LiteralPath $candidate).Path
                break
            }
        }

        if ($null -eq $sourceFile) {
            $operand = $null
            if ($payload.PSObject.Properties.Name -contains "operand") {
                $operand = [string]$payload.operand
            } elseif ($payload.PSObject.Properties.Name -contains "blocks") {
                $entry = $payload.blocks | Select-Object -First 1
                if ($null -ne $entry -and $entry.PSObject.Properties.Name -contains "expressions") {
                    $ret = $entry.expressions | Where-Object {
                        $_.PSObject.Properties.Name -contains "kind" -and [string]$_.kind -eq "return"
                    } | Select-Object -First 1
                    if ($null -ne $ret -and $ret.PSObject.Properties.Name -contains "operand") {
                        $operand = [string]$ret.operand
                    }
                }
            }

            $isReturnScenario = [string]::IsNullOrWhiteSpace($scenario) -or $scenario -eq "return"
            if (-not $isReturnScenario) {
                $skipped.Add("$sample (scenario=$scenario, missing companion .cj source)")
                continue
            }
            if ([string]::IsNullOrWhiteSpace($operand)) {
                $skipped.Add("$sample (missing return operand and missing companion .cj source)")
                continue
            }

            $sampleTmp = Join-Path $tmpRoot $sample
            New-Item -ItemType Directory -Path $sampleTmp -Force | Out-Null
            $generatedSource = Join-Path $sampleTmp "main.cj"
            @"
main(): Int32 {
    return $operand
}
"@ | Set-Content -LiteralPath $generatedSource -Encoding UTF8
            $sourceFile = $generatedSource
        }

        $sampleTmp = Join-Path $tmpRoot $sample
        $tempsDir = Join-Path $sampleTmp "temps"
        New-Item -ItemType Directory -Path $sampleTmp -Force | Out-Null
        New-Item -ItemType Directory -Path $tempsDir -Force | Out-Null

        $exePath = Join-Path $sampleTmp "main.exe"
        & $cjcPath $sourceFile --output $exePath --save-temps $tempsDir ("-" + $OptimizationLevel)
        $optBcFile = Get-ChildItem -Path $tempsDir -Filter "*.opt.bc" -File | Select-Object -First 1
        if ($LASTEXITCODE -ne 0 -and $null -eq $optBcFile) {
            throw "cjc failed for $sample"
        }
        if ($null -eq $optBcFile) {
            throw "missing .opt.bc for ${sample}: $tempsDir"
        }
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Warning: cjc link failed for ${sample}, but .opt.bc exists; continuing baseline generation."
        }
        $optBc = $optBcFile.FullName

        $targetTxt = Join-Path $targetDir "$sample.txt"
        & $llvmDisPath $optBc -o $targetTxt
        if ($LASTEXITCODE -ne 0) {
            throw "llvm-dis failed for $sample"
        }
        $generated++
    }
} finally {
    if (Test-Path $tmpRoot) {
        Remove-Item -Recurse -Force $tmpRoot
    }
}

Write-Host "Generated official LLVM baselines: $generated (opt=$OptimizationLevel, root=$OutputRootDirName)"
if ($skipped.Count -gt 0) {
    Write-Host "Skipped fixtures:"
    $skipped | ForEach-Object { Write-Host "  - $_" }
}
