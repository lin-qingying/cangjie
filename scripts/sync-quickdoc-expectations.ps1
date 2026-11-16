[CmdletBinding()]
param(
    [switch]$Check
)

$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $repositoryRoot 'code-insight\highlighting\testResources\documentation\expectations.json'
$destinationPaths = @(
    (Join-Path $repositoryRoot 'lsp\testResources\documentation\expectations.json'),
    (Join-Path $repositoryRoot 'intellij-ide\modules\ide\base\src\test\resources\documentation\expectations.json')
)

if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
    throw "Quick Doc expectations source does not exist: $sourcePath"
}

if (-not $Check) {
    foreach ($destinationPath in $destinationPaths) {
        $destinationDirectory = Split-Path -Parent $destinationPath
        if (-not (Test-Path -LiteralPath $destinationDirectory -PathType Container)) {
            New-Item -ItemType Directory -Path $destinationDirectory -Force | Out-Null
        }
        Copy-Item -LiteralPath $sourcePath -Destination $destinationPath -Force
        Write-Output "Synchronized $destinationPath"
    }
    exit 0
}

$sourceBytes = [System.IO.File]::ReadAllBytes($sourcePath)
foreach ($destinationPath in $destinationPaths) {
    if (-not (Test-Path -LiteralPath $destinationPath -PathType Leaf)) {
        throw "Quick Doc expectations copy is missing: $destinationPath"
    }

    $destinationBytes = [System.IO.File]::ReadAllBytes($destinationPath)
    $matchesSource = $sourceBytes.Length -eq $destinationBytes.Length
    for ($index = 0; $matchesSource -and $index -lt $sourceBytes.Length; $index++) {
        if ($sourceBytes[$index] -ne $destinationBytes[$index]) {
            $matchesSource = $false
        }
    }

    if (-not $matchesSource) {
        throw "Quick Doc expectations differ from the source: $destinationPath"
    }
    Write-Output "Verified $destinationPath"
}
