# Isolation matrix: std-handle mode x working directory.
#   mode=inherit : STARTUPINFO.dwFlags = 0, child inherits whatever the caller has
#   mode=nul     : all three std handles = NUL device
#   mode=files   : stdin=NUL, stdout/stderr = real files
#   mode=pipes   : all three std handles = anonymous pipes (what ProcessBuilder does)
$ErrorActionPreference = 'Stop'
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
public static class MX {
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
  [DllImport("kernel32.dll", SetLastError=true)] public static extern IntPtr GetStdHandle(int which);

  const uint GENERIC_READ = 0x80000000, GENERIC_WRITE = 0x40000000;
  const uint FILE_SHARE_RW = 0x3;
  const uint OPEN_ALWAYS = 4;
  const uint FILE_ATTRIBUTE_NORMAL = 0x80;
  const int STARTF_USESTDHANDLES = 0x100;

  static IntPtr OpenFile(string path, bool write) {
    SA sa = new SA(); sa.nLength = Marshal.SizeOf(typeof(SA)); sa.inherit = true;
    IntPtr p = Marshal.AllocHGlobal(Marshal.SizeOf(typeof(SA)));
    Marshal.StructureToPtr(sa, p, false);
    IntPtr h = CreateFileW(path, write ? GENERIC_WRITE : GENERIC_READ, FILE_SHARE_RW, p, OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, IntPtr.Zero);
    Marshal.FreeHGlobal(p);
    return h;
  }

  public static string Run(string exe, string argv4, string cwd, string mode, string outFile, string errFile, int waitMs) {
    SA sa = new SA(); sa.nLength = Marshal.SizeOf(typeof(SA)); sa.inherit = true;
    IntPtr pr,cw,cr,pw;
    if (!CreatePipe(out pr,out cw,ref sa,0)) return "CreatePipe1 err="+Marshal.GetLastWin32Error();
    if (!CreatePipe(out cr,out pw,ref sa,0)) return "CreatePipe2 err="+Marshal.GetLastWin32Error();

    SI si = new SI(); si.cb = Marshal.SizeOf(typeof(SI));
    IntPtr inH = IntPtr.Zero, outH = IntPtr.Zero, errH = IntPtr.Zero;
    if (mode == "nul") {
      inH = OpenFile("NUL", false); outH = OpenFile("NUL", true); errH = OpenFile("NUL", true);
      si.dwFlags = STARTF_USESTDHANDLES;
    } else if (mode == "files") {
      inH = OpenFile("NUL", false); outH = OpenFile(outFile, true); errH = OpenFile(errFile, true);
      si.dwFlags = STARTF_USESTDHANDLES;
    } else if (mode == "pipes") {
      IntPtr a,b,c,d,e,f;
      CreatePipe(out a,out b,ref sa,0); CreatePipe(out c,out d,ref sa,0); CreatePipe(out e,out f,ref sa,0);
      inH = b; outH = d; errH = f;   // child ends
      si.dwFlags = STARTF_USESTDHANDLES;
    }
    si.hIn = inH; si.hOut = outH; si.hErr = errH;

    string cmd = "\""+exe+"\" "+cr.ToInt64()+" "+cw.ToInt64()+" 1 \""+argv4+"\"";
    PI pi;
    bool ok = CreateProcessW(null, cmd, IntPtr.Zero, IntPtr.Zero, true, 0, IntPtr.Zero, cwd, ref si, out pi);
    if (mode == "files" || mode == "nul") { if (inH!=IntPtr.Zero) CloseHandle(inH); if (outH!=IntPtr.Zero) CloseHandle(outH); if (errH!=IntPtr.Zero) CloseHandle(errH); }
    if(!ok) return "CreateProcess err="+Marshal.GetLastWin32Error();
    CloseHandle(cr); CloseHandle(cw);
    uint wait = WaitForSingleObject(pi.hProc, (uint)waitMs);
    uint code; GetExitCodeProcess(pi.hProc, out code);
    if (wait != 258) TerminateProcess(pi.hProc, 1);
    CloseHandle(pr); CloseHandle(pw); CloseHandle(pi.hThread); CloseHandle(pi.hProc);
    return "wait="+wait+" exit=0x"+code.ToString("X8");
  }
}
"@

$sdk  = 'C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5'
$exe  = Join-Path $sdk 'tools\bin\LSPMacroServer.exe'
$cjc  = Join-Path $sdk 'bin\cjc.exe'
$dir  = $PSScriptRoot

$env:Path | Out-File (Join-Path $dir 'env_path.txt') -Encoding UTF8
Get-ChildItem env: | Sort-Object Name | ForEach-Object { "$($_.Name)=$($_.Value)" } |
  Set-Content (Join-Path $dir 'env_all.txt') -Encoding UTF8

$cwds = @(
  @{ n='toolsbin'; c=(Join-Path $sdk 'tools\bin') },
  @{ n='sdk';      c=$sdk }
)
$lines = @()
foreach ($mode in @('inherit','nul','files','pipes')) {
  foreach ($k in $cwds) {
    $o = Join-Path $dir ("o_"+$mode+"_"+$k.n+".txt")
    $e = Join-Path $dir ("e_"+$mode+"_"+$k.n+".txt")
    Remove-Item $o,$e -ErrorAction SilentlyContinue
    $r = [MX]::Run($exe, $cjc, $k.c, $mode, $o, $e, 6000)
    $lines += "[$mode/$($k.n)] -> $r"
  }
}
$lines | Set-Content (Join-Path $dir 'matrix.log') -Encoding UTF8
$lines
