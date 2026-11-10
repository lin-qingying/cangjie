# Minimal debugger: catch the first-chance access violation in a Cangjie binary,
# dump the faulting module+offset, registers, the offending pointer's memory,
# and candidate return addresses found on the stack.
$ErrorActionPreference = 'Stop'
Add-Type -TypeDefinition @"
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;

public static class Dbg {
  [StructLayout(LayoutKind.Sequential)] public struct SA { public int nLength; public IntPtr sd; public bool inherit; }
  [StructLayout(LayoutKind.Sequential)]
  public struct PI { public IntPtr hProcess, hThread; public int pid, tid; }
  [DllImport("kernel32.dll", SetLastError=true, CharSet=CharSet.Unicode)]
  public static extern bool CreateProcessW(string app, string cmd, IntPtr pa, IntPtr ta, bool inherit,
      int flags, IntPtr env, string cwd, IntPtr si, ref PI pi);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool WaitForDebugEvent(IntPtr ev, uint ms);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool ContinueDebugEvent(int pid, int tid, int status);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool GetThreadContext(IntPtr hThread, IntPtr ctx);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern IntPtr OpenThread(int access, bool inherit, int tid);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool CloseHandle(IntPtr h);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool ReadProcessMemory(IntPtr hProc, IntPtr addr, byte[] buf, IntPtr size, out IntPtr read);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool TerminateProcess(IntPtr hProc, uint code);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool GetExitCodeProcess(IntPtr hProc, out uint code);
  [DllImport("kernel32.dll", SetLastError=true)] public static extern bool VirtualProtectEx(IntPtr hProc, IntPtr addr, IntPtr size, uint prot, out uint old);

  const int DEBUG_ONLY_THIS_PROCESS = 0x2;
  const int DBG_CONTINUE = 0x00010002;
  const int EXCEPTION_DEBUG_EVENT = 1, CREATE_PROCESS_DEBUG_EVENT = 3, LOAD_DLL_DEBUG_EVENT = 6, EXIT_PROCESS_DEBUG_EVENT = 5;

  static string BaseName(string p) { if (string.IsNullOrEmpty(p)) return p; int i = p.LastIndexOfAny(new char[]{char.Parse("\\"), char.Parse("/")}); return i >= 0 ? p.Substring(i+1) : p; }
  static IntPtr Ctx() {
    IntPtr raw = Marshal.AllocHGlobal(0x500 + 16);
    long aligned = ((long)raw + 15) & ~15L;
    return new IntPtr(aligned);
  }
  static long Reg(IntPtr ctx, int off) { return Marshal.ReadInt64(ctx, off); }

  static string ReadWStr(IntPtr h, IntPtr addr, int max) {
    byte[] b = new byte[max * 2];
    IntPtr got;
    if (!ReadProcessMemory(h, addr, b, (IntPtr)b.Length, out got) || got.ToInt64() <= 0) return "<unreadable>";
    int n = 0; StringBuilder sb = new StringBuilder();
    while (n < max) { char c = (char)(b[n*2] | (b[n*2+1] << 8)); if (c == '\0') break; sb.Append(c); n++; }
    return sb.ToString();
  }

  public static string Run(string exe, string cmdline, string cwd) {
    PI pi = new PI();
    IntPtr si = Marshal.AllocHGlobal(104);
    for (int i = 0; i < 104; i++) Marshal.WriteByte(si, i, 0);
    Marshal.WriteInt32(si, 0, 104);   // cb
    bool ok = CreateProcessW(exe, cmdline, IntPtr.Zero, IntPtr.Zero, false,
        DEBUG_ONLY_THIS_PROCESS, IntPtr.Zero, cwd, si, ref pi);
    if (!ok) return "CreateProcess err=" + Marshal.GetLastWin32Error();

    IntPtr ev = Marshal.AllocHGlobal(256);
    var mods = new List<KeyValuePair<long, string>>();
    mods.Add(new KeyValuePair<long, string>(0, "<exe-image>"));

    for (int guard = 0; guard < 10000; guard++) {
      if (!WaitForDebugEvent(ev, 30000)) return "WaitForDebugEvent timeout/err=" + Marshal.GetLastWin32Error();
      int code = Marshal.ReadInt32(ev, 0);
      int pid = Marshal.ReadInt32(ev, 4);
      int tid = Marshal.ReadInt32(ev, 8);

      if (code == LOAD_DLL_DEBUG_EVENT) {
        long b = Marshal.ReadIntPtr(ev, 24).ToInt64();
        IntPtr namePtr = Marshal.ReadIntPtr(ev, 40);
        string name = "<noname>";
        if (namePtr != IntPtr.Zero) {
          byte[] us = new byte[16]; IntPtr gotName;
          if (ReadProcessMemory(pi.hProcess, namePtr, us, (IntPtr)16, out gotName)) {
            long strPtr = BitConverter.ToInt64(us, 8);   // UNICODE_STRING.Buffer
            if (strPtr != 0) name = ReadWStr(pi.hProcess, (IntPtr)strPtr, 260);
          }
        }
        mods.Add(new KeyValuePair<long, string>(b, name));
        ContinueDebugEvent(pid, tid, DBG_CONTINUE);
        continue;
      }
      if (code == EXCEPTION_DEBUG_EVENT) {
        int exCode = Marshal.ReadInt32(ev, 16);
        long exAddr = Marshal.ReadIntPtr(ev, 32).ToInt64();
        int nParam = Marshal.ReadInt32(ev, 40);
        long p0 = nParam > 0 ? Marshal.ReadIntPtr(ev, 48).ToInt64() : 0;
        long p1 = nParam > 1 ? Marshal.ReadIntPtr(ev, 56).ToInt64() : 0;

        if ((uint)exCode == 0x80000003u) { ContinueDebugEvent(pid, tid, DBG_CONTINUE); continue; } // initial bp
        if ((uint)exCode == 0x40010006u) { ContinueDebugEvent(pid, tid, DBG_CONTINUE); continue; } // output string

        IntPtr th = OpenThread(0x0008 | 0x0002 | 0x0004, false, tid); // GET_CONTEXT|SET_CONTEXT|SUSPEND
        IntPtr ctx = Ctx();
        for (int i = 0; i < 0x4D0; i++) Marshal.WriteByte(ctx, i, 0);
        Marshal.WriteInt32(ctx, 0x30, 0x0010000B);
        bool gotCtx = GetThreadContext(th, ctx);

        long rip = Reg(ctx, 0xF8), rsp = Reg(ctx, 0x98);
        long rcx = Reg(ctx, 0x80), rdx = Reg(ctx, 0x88), r8 = Reg(ctx, 0xB8);
        long r12 = Reg(ctx, 0xD8), r13 = Reg(ctx, 0xE0);

        // map rip to loaded module
        string ripMod = "?"; long ripOff = 0;
        long best = -1;
        foreach (var m in mods) if (m.Key <= rip && m.Key > best) { best = m.Key; ripMod = m.Value; }
        ripOff = rip - best;

        // scan stack for return addresses inside loaded modules
        var frames = new List<string>();
        byte[] stack = new byte[0x600];
        IntPtr got;
        if (ReadProcessMemory(pi.hProcess, (IntPtr)rsp, stack, (IntPtr)stack.Length, out got)) {
          for (int i = 0; i < stack.Length / 8 && frames.Count < 14; i++) {
            long v = BitConverter.ToInt64(stack, i * 8);
            long mb = -1; string mn = null;
            foreach (var m in mods) if (m.Key <= v && m.Key > mb) { mb = m.Key; mn = m.Value; }
            if (mn != null && v - mb > 0x1000) frames.Add(string.Format("  [rsp+0x{0:X3}] {1}+0x{2:X}", i*8, BaseName(mn), v - mb));
          }
        }

        // bytes at the faulting pointer and at rcx
        var sb = new StringBuilder();
        sb.AppendLine("EXCEPTION 0x" + ((uint)exCode).ToString("X8") + " at RIP=" + rip.ToString("X16") + " (" + BaseName(ripMod) + "+0x" + ripOff.ToString("X") + ")");
        sb.AppendLine("  access=" + p0 + " faultAddr=0x" + p1.ToString("X") + " nParam=" + nParam);
        sb.AppendLine("  rcx=0x" + rcx.ToString("X") + " rdx=0x" + rdx.ToString("X") + " r8=0x" + r8.ToString("X") + " r12=0x" + r12.ToString("X") + " r13=0x" + r13.ToString("X") + " rsp=0x" + rsp.ToString("X"));
        sb.AppendLine("  wstr@rcx = " + (rcx > 0x10000 ? "'" + ReadWStr(pi.hProcess, (IntPtr)rcx, 80) + "'" : "<not readable>"));
        sb.AppendLine("  wstr@faultAddr = " + (p1 > 0x10000 ? "'" + ReadWStr(pi.hProcess, (IntPtr)p1, 40) + "'" : "<not readable>"));
        sb.AppendLine("  modules loaded: " + mods.Count);
        sb.AppendLine("  stack candidates:");
        foreach (var f in frames) sb.AppendLine(f);

        // Walk the PEB module list from the crashed thread's node and describe each entry.
        Func<IntPtr, IntPtr, byte[]> rd = (IntPtr h, IntPtr a) => { byte[] b = new byte[8]; IntPtr g; return ReadProcessMemory(h, a, b, (IntPtr)8, out g) ? b : null; };
        long node = r12;
        var walk = new StringBuilder();
        walk.AppendLine("  module-list walk from crashed node:");
        for (int k = 0; k < 60 && node != 0; k++) {
          byte[] nb = rd(pi.hProcess, (IntPtr)node);
          if (nb == null) { walk.AppendLine("    [" + k + "] node=0x" + node.ToString("X") + " <unreadable>"); break; }
          long next = BitConverter.ToInt64(nb, 0);
          long dllBase = 0, sizeImg = 0;
          byte[] f1 = rd(pi.hProcess, (IntPtr)(node + 0x20)); if (f1 != null) dllBase = BitConverter.ToInt64(f1, 0);
          byte[] f2 = rd(pi.hProcess, (IntPtr)(node + 0x30)); if (f2 != null) sizeImg = BitConverter.ToInt64(f2, 0);
          long baseBuf = 0; int baseLen = -1;
          byte[] f3 = rd(pi.hProcess, (IntPtr)(node + 0x48));
          if (f3 != null) {
            baseLen = BitConverter.ToUInt16(f3, 0);
            byte[] f4 = rd(pi.hProcess, (IntPtr)(node + 0x50));
            if (f4 != null) baseBuf = BitConverter.ToInt64(f4, 0);
          }
          long fullBuf = 0; byte[] f5 = rd(pi.hProcess, (IntPtr)(node + 0x38));
          if (f5 != null) { byte[] f6 = rd(pi.hProcess, (IntPtr)(node + 0x40)); if (f6 != null) fullBuf = BitConverter.ToInt64(f6, 0); }
          string nm = baseBuf > 0x10000 ? ReadWStr(pi.hProcess, (IntPtr)baseBuf, 120) : "<bad buffer>";
          string marker = (node == r12) ? "  <<== crashed here" : "";
          walk.AppendLine(string.Format("    [{0}] node=0x{1:X} dllBase=0x{2:X} size=0x{3:X} nameLen={4} nameBuf=0x{5:X} name='{6}'{7}",
              k, node, dllBase, sizeImg, baseLen, baseBuf, nm, marker));
          if (next == node) break;
          node = next;
        }
        sb.Append(walk.ToString());
        TerminateProcess(pi.hProcess, 3);
        CloseHandle(th);
        return sb.ToString();
      }
      if (code == EXIT_PROCESS_DEBUG_EVENT) {
        uint ec; GetExitCodeProcess(pi.hProcess, out ec);
        return "process exited cleanly, code=0x" + ec.ToString("X8");
      }
      ContinueDebugEvent(pid, tid, DBG_CONTINUE);
    }
    return "guard exhausted";
  }
}
"@

$sdk = 'C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5'
$cjpm = Join-Path $sdk 'tools\bin\cjpm.exe'
$res = [Dbg]::Run($cjpm, ('"' + $cjpm + '" --version'), $sdk)
$res | Set-Content (Join-Path $PSScriptRoot 'dbg_cjpm.log') -Encoding UTF8
$res


