# Reproduce the plugin's JNA launch and bisect which parameter breaks it on this OS build.
$ErrorActionPreference = 'Stop'
Add-Type -TypeDefinition @"
using System;
using System.Collections;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;

public static class PL {
  [StructLayout(LayoutKind.Sequential)] public struct SA { public int nLength; public IntPtr sd; public bool inherit; }
  [StructLayout(LayoutKind.Sequential)]
  public struct SI {
    public int cb; public IntPtr lpReserved, lpDesktop, lpTitle;
    public int dwX, dwY, dwXSize, dwYSize, dwXCountChars, dwYCountChars, dwFillAttribute, dwFlags;
    public short wShowWindow, cbReserved2; public IntPtr lpReserved2, hIn, hOut, hErr;
  }
  [StructLayout(LayoutKind.Sequential)] public struct PI { public IntPtr hProc, hThread; public int pid, tid; }
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool CreatePipe(out IntPtr r, out IntPtr w, ref SA sa, int size);
  [DllImport("kernel32.dll", SetLastError=true, CharSet=CharSet.Unicode)]
  public static extern bool CreateProcessW(string app, string cmd, IntPtr pa, IntPtr ta, bool inherit, int flags, IntPtr env, string cwd, ref SI si, out PI pi);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern uint WaitForSingleObject(IntPtr h, uint ms);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool GetExitCodeProcess(IntPtr h, out uint code);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool CloseHandle(IntPtr h);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool TerminateProcess(IntPtr h, uint code);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern IntPtr GetStdHandle(int which);
  [DllImport("kernel32.dll", SetLastError=true, CharSet=CharSet.Unicode)]
  public static extern IntPtr CreateFileW(string name, uint access, uint share, IntPtr sa, uint disp, uint flags, IntPtr tmpl);

  const int STARTF_USESTDHANDLES = 0x100;
  const int CREATE_UNICODE_ENVIRONMENT = 0x400;
  const int CREATE_NO_WINDOW = 0x08000000;
  const int STD_INPUT = -10, STD_OUTPUT = -11, STD_ERROR = -12;

  static IntPtr EnvBlock(IDictionary env, string sdk) {
    var sb = new StringBuilder();
    var keys = new List<string>();
    foreach (DictionaryEntry e in env) keys.Add((string)e.Key);
    keys.Sort(StringComparer.OrdinalIgnoreCase);
    foreach (var k in keys) { sb.Append(k).Append('=').Append((string)env[k]).Append('\0'); }
    sb.Append("CANGJIE_HOME=").Append(sdk).Append('\0');
    sb.Append('\0');
    byte[] b = Encoding.Unicode.GetBytes(sb.ToString());
    IntPtr p = Marshal.AllocHGlobal(b.Length);
    Marshal.Copy(b, 0, p, b.Length);
    return p;
  }

  public static string SdkHome = "";
  public static string Run(string exe, string argv4, string cwd, string mode, int waitMs) {
    SA sa = new SA(); sa.nLength = Marshal.SizeOf(typeof(SA)); sa.inherit = true;
    IntPtr pr, cw, cr, pw;
    CreatePipe(out pr, out cw, ref sa, 0);
    CreatePipe(out cr, out pw, ref sa, 0);

    SI si = new SI(); si.cb = Marshal.SizeOf(typeof(SI));
    int flags = 0;
    IntPtr envBlock = IntPtr.Zero;
    bool useCwd = cwd != null;

    switch (mode) {
      case "official":            // cjc's own launch
        si.dwFlags = STARTF_USESTDHANDLES;
        si.hOut = GetStdHandle(STD_ERROR); si.hErr = GetStdHandle(STD_ERROR);
        break;
      case "plugin":              // current Kotlin code
        flags = CREATE_UNICODE_ENVIRONMENT | CREATE_NO_WINDOW;
        envBlock = EnvBlock(Environment.GetEnvironmentVariables(), SdkHome);
        break;
      case "plugin_nocwd":
        flags = CREATE_UNICODE_ENVIRONMENT | CREATE_NO_WINDOW;
        envBlock = EnvBlock(Environment.GetEnvironmentVariables(), SdkHome);
        useCwd = false;
        break;
      case "plugin_noenv":
        flags = CREATE_NO_WINDOW;
        break;
      case "plugin_noconsole":
        flags = CREATE_UNICODE_ENVIRONMENT;
        envBlock = EnvBlock(Environment.GetEnvironmentVariables(), SdkHome);
        break;
      case "plugin_nullstd":      // STARTF with null/invalid std handles, like a GUI/daemon parent
        flags = CREATE_UNICODE_ENVIRONMENT | CREATE_NO_WINDOW;
        envBlock = EnvBlock(Environment.GetEnvironmentVariables(), SdkHome);
        si.dwFlags = STARTF_USESTDHANDLES;
        si.hIn = IntPtr.Zero; si.hOut = IntPtr.Zero; si.hErr = IntPtr.Zero;
        break;
      default:
        return "unknown mode";
    }

    string cmd = "\"" + exe + "\" " + cr.ToInt64() + " " + cw.ToInt64() + " 1 \"" + argv4 + "\"";
    PI pi;
    bool ok = CreateProcessW(exe, cmd, IntPtr.Zero, IntPtr.Zero, true, flags, envBlock,
        useCwd ? cwd : null, ref si, out pi);
    if (envBlock != IntPtr.Zero) Marshal.FreeHGlobal(envBlock);
    if (!ok) return "CreateProcess err=" + Marshal.GetLastWin32Error();
    CloseHandle(cr); CloseHandle(cw);

    uint wait = WaitForSingleObject(pi.hProc, (uint)waitMs);
    uint code; GetExitCodeProcess(pi.hProc, out code);
    if (wait == 258) TerminateProcess(pi.hProc, 1);
    CloseHandle(pr); CloseHandle(pw); CloseHandle(pi.hThread); CloseHandle(pi.hProc);
    return wait == 258 ? "ALIVE" : ("exit=0x" + code.ToString("X8"));
  }

  static string argv4Sdk(string p) { return null; }
}
"@

$sdk = 'C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5'; [PL]::SdkHome = $sdk
$exe = Join-Path $sdk 'tools\bin\LSPMacroServer.exe'
$cjc = Join-Path $sdk 'bin\cjc.exe'
$cwd = Join-Path $sdk 'tools\bin'

# official passes the cjc.exe path as argv[4]; the plugin passes the exe's parent dir
$cases = @(
  @{ n='official(cjc-style)'; m='official';     a=$cjc },
  @{ n='plugin(cwd+env+nowin)'; m='plugin';     a=$cwd },
  @{ n='plugin, no cwd';        m='plugin_nocwd'; a=$cwd },
  @{ n='plugin, no custom env'; m='plugin_noenv'; a=$cwd },
  @{ n='plugin, no NO_WINDOW';  m='plugin_noconsole'; a=$cwd },
  @{ n='plugin, null std';      m='plugin_nullstd'; a=$cwd }
)
foreach ($k in $cases) {
  $r = [PL]::Run($exe, $k.a, $cwd, $k.m, 6000)
  "{0,-24} -> {1}" -f $k.n, $r
}
