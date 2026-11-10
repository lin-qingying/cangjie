# Cross-SDK check with each SDK's own clean environment, so PATH cannot mix SDKs.
$ErrorActionPreference = 'Stop'

function Invoke-ExeClean($exe, $argList, $wd, $envPairs, $timeoutMs) {
  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = $exe; $psi.Arguments = $argList; $psi.WorkingDirectory = $wd
  $psi.UseShellExecute = $false
  $psi.RedirectStandardOutput = $true; $psi.RedirectStandardError = $true
  $psi.EnvironmentVariables.Clear()
  foreach ($k in $envPairs.Keys) { $psi.EnvironmentVariables[$k] = $envPairs[$k] }
  $p = New-Object System.Diagnostics.Process; $p.StartInfo = $psi
  [void]$p.Start()
  $so = $p.StandardOutput.ReadToEndAsync(); $se = $p.StandardError.ReadToEndAsync()
  if (-not $p.WaitForExit($timeoutMs)) { try { $p.Kill() } catch {}; return "TIMEOUT" }
  $p.WaitForExit()
  "exit=0x{0:X8} out=[{1}] err=[{2}]" -f $p.ExitCode, $so.Result.Trim(), $se.Result.Trim()
}

$sdks = @(
  @{ n='1.0.0'; root='C:\Users\lin17\.cangjie\sdks\cangjie-1.0.0' },
  @{ n='1.0.5'; root='C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5' },
  @{ n='1.1.3'; root='C:\Users\lin17\.cangjie\sdks\cangjie-sdk-windows-x64-1.1.3\cangjie' }
)
foreach ($s in $sdks) {
  $cjpm = Join-Path $s.root 'tools\bin\cjpm.exe'
  if (-not (Test-Path $cjpm)) { Write-Output "[$($s.n)] no cjpm"; continue }
  $rt = Get-ChildItem (Join-Path $s.root 'runtime\lib') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
  $pathParts = @((Join-Path $s.root 'bin'), (Join-Path $s.root 'tools\bin'))
  if ($rt) { $pathParts += $rt.FullName }
  $envPairs = @{
    'SystemRoot'   = $env:SystemRoot
    'SystemDrive'  = $env:SystemDrive
    'TEMP'         = $env:TEMP
    'TMP'          = $env:TMP
    'PATH'         = ($pathParts -join ';')
    'CANGJIE_HOME' = $s.root
  }
  Write-Output ("[{0}] cjpm --version -> {1}" -f $s.n, (Invoke-ExeClean $cjpm '--version' $s.root $envPairs 25000))
}
