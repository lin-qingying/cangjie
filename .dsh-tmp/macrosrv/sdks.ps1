# Is the runtime-init crash SDK-specific or machine-wide?
# Any Cangjie-compiled executable initialises the runtime, so cjpm --version is
# a minimal, launcher-independent probe.
$ErrorActionPreference = 'Stop'

function Invoke-Exe($file, $argList, $wd, $timeoutMs) {
  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = $file; $psi.Arguments = $argList; $psi.WorkingDirectory = $wd
  $psi.UseShellExecute = $false
  $psi.RedirectStandardOutput = $true; $psi.RedirectStandardError = $true
  $p = New-Object System.Diagnostics.Process; $p.StartInfo = $psi
  [void]$p.Start()
  $so = $p.StandardOutput.ReadToEndAsync(); $se = $p.StandardError.ReadToEndAsync()
  if (-not $p.WaitForExit($timeoutMs)) { try { $p.Kill() } catch {}; return "TIMEOUT" }
  $p.WaitForExit()
  "exit=0x{0:X8} out=[{1}] err=[{2}]" -f $p.ExitCode, $so.Result.Trim(), $se.Result.Trim()
}

$candidates = @(
  @{ n='0.53.18'; p='C:\Users\lin17\.cangjie\sdks\cangjie-0.53.18\tools\bin\cjpm.exe' },
  @{ n='1.0.0';   p='C:\Users\lin17\.cangjie\sdks\cangjie-1.0.0\tools\bin\cjpm.exe' },
  @{ n='1.0.5';   p='C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\tools\bin\cjpm.exe' },
  @{ n='1.1.3';   p='C:\Users\lin17\.cangjie\sdks\cangjie-sdk-windows-x64-1.1.3\cangjie\tools\bin\cjpm.exe' }
)
foreach ($c in $candidates) {
  if (Test-Path $c.p) {
    $wd = Split-Path (Split-Path (Split-Path $c.p -Parent) -Parent) -Parent
    Write-Output ("[{0}] cjpm --version -> {1}" -f $c.n, (Invoke-Exe $c.p '--version' $wd 25000))
  } else {
    Write-Output ("[{0}] missing: {1}" -f $c.n, $c.p)
  }
}
