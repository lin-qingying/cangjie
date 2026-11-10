# Environment-block experiment: does the crash depend on which environment the
# child inherits, rather than on its working directory?
$ErrorActionPreference = 'Stop'
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
using System.Text;
public static class EP {
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
  const uint GENERIC_READ=0x80000000, GENERIC_WRITE=0x40000000, FILE_SHARE_RW=0x3, OPEN_ALWAYS=4, FILE_ATTRIBUTE_NORMAL=0x80;
  const int STARTF_USESTDHANDLES = 0x100;
  const int CREATE_UNICODE_ENVIRONMENT = 0x400;

  static IntPtr OpenFile(string path, bool write) {
    SA sa = new SA(); sa.nLength = Marshal.SizeOf(typeof(SA)); sa.inherit = true;
    IntPtr p = Marshal.AllocHGlobal(Marshal.SizeOf(typeof(SA)));
    Marshal.StructureToPtr(sa, p, false);
    IntPtr h = CreateFileW(path, write?GENERIC_WRITE:GENERIC_READ, FILE_SHARE_RW, p, OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, IntPtr.Zero);
    Marshal.FreeHGlobal(p);
    return h;
  }

  // envLines: null => inherit; otherwise "K=V" entries.
  public static string Run(string exe, string argv4, string cwd, string[] envLines, string outFile, string errFile, int waitMs) {
    SA sa = new SA(); sa.nLength = Marshal.SizeOf(typeof(SA)); sa.inherit = true;
    IntPtr pr,cw,cr,pw;
    if (!CreatePipe(out pr,out cw,ref sa,0)) return "CreatePipe1 err="+Marshal.GetLastWin32Error();
    if (!CreatePipe(out cr,out pw,ref sa,0)) return "CreatePipe2 err="+Marshal.GetLastWin32Error();

    SI si = new SI(); si.cb = Marshal.SizeOf(typeof(SI));
    IntPtr inH = OpenFile("NUL", false), outH = OpenFile(outFile, true), errH = OpenFile(errFile, true);
    si.hIn = inH; si.hOut = outH; si.hErr = errH; si.dwFlags = STARTF_USESTDHANDLES;

    IntPtr envBlock = IntPtr.Zero;
    int flags = 0;
    if (envLines != null) {
      StringBuilder sb = new StringBuilder();
      foreach (string line in envLines) { sb.Append(line); sb.Append('\0'); }
      sb.Append('\0');
      byte[] bytes = Encoding.Unicode.GetBytes(sb.ToString());
      envBlock = Marshal.AllocHGlobal(bytes.Length);
      Marshal.Copy(bytes, 0, envBlock, bytes.Length);
      flags = CREATE_UNICODE_ENVIRONMENT;
    }

    string cmd = "\""+exe+"\" "+cr.ToInt64()+" "+cw.ToInt64()+" 1 \""+argv4+"\"";
    PI pi;
    bool ok = CreateProcessW(null, cmd, IntPtr.Zero, IntPtr.Zero, true, flags, envBlock, cwd, ref si, out pi);
    CloseHandle(inH); CloseHandle(outH); CloseHandle(errH);
    if (envBlock != IntPtr.Zero) Marshal.FreeHGlobal(envBlock);
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
$cwd  = Join-Path $sdk 'tools\bin'
$dir  = $PSScriptRoot

$minimal = @(
  "SystemRoot=$env:SystemRoot",
  "SystemDrive=$env:SystemDrive",
  "TEMP=$env:TEMP",
  "TMP=$env:TMP"
)
$withSdk = $minimal + @(
  "CANGJIE_HOME=$sdk",
  "PATH=$(Join-Path $sdk 'bin');$(Join-Path $sdk 'tools\bin');$(Join-Path $sdk 'runtime\lib\windows_x86_64_cjnative')"
)
$withSdkNoHome = $minimal + @(
  "PATH=$(Join-Path $sdk 'bin');$(Join-Path $sdk 'tools\bin');$(Join-Path $sdk 'runtime\lib\windows_x86_64_cjnative')"
)

$cases = @(
  @{ n='inherit';       e=$null },
  @{ n='minimal';       e=$minimal },
  @{ n='sdk_home_path'; e=$withSdk },
  @{ n='sdk_path_only'; e=$withSdkNoHome }
)
$lines = @()
foreach ($k in $cases) {
  $o = Join-Path $dir ("eo_"+$k.n+".txt"); $e = Join-Path $dir ("ee_"+$k.n+".txt")
  Remove-Item $o,$e -ErrorAction SilentlyContinue
  $r = [EP]::Run($exe, $cjc, $cwd, $k.e, $o, $e, 6000)
  $lines += "[$($k.n)] -> $r"
}
$lines | Set-Content (Join-Path $dir 'envprobe.log') -Encoding UTF8
$lines
