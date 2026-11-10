# Launch LSPMacroServer with the official argv shape but with the child's
# stdout/stderr redirected to files, so the server's own Errorln() output is visible.
$ErrorActionPreference = 'Stop'
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
public static class SP {
  [StructLayout(LayoutKind.Sequential)] public struct SA { public int nLength; public IntPtr sd; public bool inherit; }
  [StructLayout(LayoutKind.Sequential)]
  public struct SI {
    public int cb;
    public IntPtr lpReserved, lpDesktop, lpTitle;
    public int dwX, dwY, dwXSize, dwYSize, dwXCountChars, dwYCountChars, dwFillAttribute, dwFlags;
    public short wShowWindow, cbReserved2;
    public IntPtr lpReserved2, hIn, hOut, hErr;
  }
  [StructLayout(LayoutKind.Sequential)] public struct PI { public IntPtr hProc,hThread; public int pid,tid; }
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool CreatePipe(out IntPtr r, out IntPtr w, ref SA sa, int size);
  [DllImport("kernel32.dll", SetLastError=true, CharSet=CharSet.Unicode)]
  public static extern bool CreateProcessW(string app, string cmd, IntPtr pa, IntPtr ta, bool inherit, int flags, IntPtr env, string cwd, ref SI si, out PI pi);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern uint WaitForSingleObject(IntPtr h, uint ms);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool GetExitCodeProcess(IntPtr h, out uint code);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool CloseHandle(IntPtr h);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool TerminateProcess(IntPtr h, uint code);
  [DllImport("kernel32.dll", SetLastError=true, CharSet=CharSet.Unicode)]
  public static extern IntPtr CreateFileW(string name, uint access, uint share, IntPtr sa, uint disp, uint flags, IntPtr tmpl);

  const uint GENERIC_WRITE = 0x40000000;
  const uint FILE_SHARE_RW = 0x3;
  const uint OPEN_ALWAYS = 4;
  const uint FILE_ATTRIBUTE_NORMAL = 0x80;
  const int STARTF_USESTDHANDLES = 0x100;

  public static IntPtr OpenInheritableFile(string path, bool write) {
    uint access = write ? GENERIC_WRITE : 0x80000000;
    SA sa = new SA(); sa.nLength = Marshal.SizeOf(typeof(SA)); sa.inherit = true;
    IntPtr p = Marshal.AllocHGlobal(Marshal.SizeOf(typeof(SA)));
    Marshal.StructureToPtr(sa, p, false);
    IntPtr h = CreateFileW(path, access, FILE_SHARE_RW, p, OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, IntPtr.Zero);
    Marshal.FreeHGlobal(p);
    return h;
  }

  public static string Run(string exe, string argv4, string cwd, string outFile, string errFile, int waitMs) {
    SA sa = new SA(); sa.nLength = Marshal.SizeOf(typeof(SA)); sa.inherit = true;
    IntPtr pr,cw,cr,pw;
    if (!CreatePipe(out pr,out cw,ref sa,0)) return "CreatePipe1 err="+Marshal.GetLastWin32Error();
    if (!CreatePipe(out cr,out pw,ref sa,0)) return "CreatePipe2 err="+Marshal.GetLastWin32Error();

    IntPtr hIn = OpenInheritableFile("NUL", false);
    IntPtr hOut = OpenInheritableFile(outFile, true);
    IntPtr hErr = OpenInheritableFile(errFile, true);
    if (hIn == new IntPtr(-1) || hOut == new IntPtr(-1) || hErr == new IntPtr(-1))
      return "CreateFile err="+Marshal.GetLastWin32Error();

    SI si = new SI();
    si.cb = Marshal.SizeOf(typeof(SI));
    si.hIn = hIn; si.hOut = hOut; si.hErr = hErr;
    si.dwFlags = STARTF_USESTDHANDLES;

    string cmd = "\""+exe+"\" "+cr.ToInt64()+" "+cw.ToInt64()+" 1 \""+argv4+"\"";
    PI pi;
    bool ok = CreateProcessW(null, cmd, IntPtr.Zero, IntPtr.Zero, true, 0, IntPtr.Zero, cwd, ref si, out pi);
    CloseHandle(hIn); CloseHandle(hOut); CloseHandle(hErr);
    if(!ok) return "CreateProcess err="+Marshal.GetLastWin32Error();
    CloseHandle(cr); CloseHandle(cw);
    uint wait = WaitForSingleObject(pi.hProc, (uint)waitMs);
    uint code; GetExitCodeProcess(pi.hProc, out code);
    if (wait != 258) { TerminateProcess(pi.hProc, 1); }
    CloseHandle(pr); CloseHandle(pw); CloseHandle(pi.hThread); CloseHandle(pi.hProc);
    return "wait="+wait+" exit=0x"+code.ToString("X8")+" ("+code+")";
  }
}
"@

$sdk  = 'C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5'
$exe  = Join-Path $sdk 'tools\bin\LSPMacroServer.exe'
$cjc  = Join-Path $sdk 'bin\cjc.exe'
$outDir = $PSScriptRoot

$cases = @(
  @{ n='cwd_sdk';     c=$sdk;                                 a=$cjc },
  @{ n='cwd_bin';     c=(Join-Path $sdk 'bin');               a=$cjc },
  @{ n='cwd_toolsbin';c=(Join-Path $sdk 'tools\bin');         a=$cjc },
  @{ n='cwd_runtimelib'; c=(Join-Path $sdk 'runtime\lib\windows_x86_64_cjnative'); a=$cjc }
)
$lines = @()
foreach ($k in $cases) {
  $o = Join-Path $outDir ("stdout_" + $k.n + ".txt")
  $e = Join-Path $outDir ("stderr_" + $k.n + ".txt")
  Remove-Item $o,$e -ErrorAction SilentlyContinue
  $r = [SP]::Run($exe, $k.a, $k.c, $o, $e, 6000)
  $lines += "[$($k.n)] argv4=$($k.a) cwd=$($k.c) -> $r"
}
$lines | Set-Content (Join-Path $outDir 'stderrprobe.log') -Encoding UTF8
$lines | ForEach-Object { $_ }
