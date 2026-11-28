"""分析 IDEA 沙箱实例的线程转储、idea.log 与 OpenTelemetry 指标。"""
import csv
import io
import os
import re
import sys
from collections import Counter, defaultdict

SANDBOX = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1"
LOG = os.path.join(SANDBOX, "log_runIde")


def parse_thread_dump(path):
    with io.open(path, "r", encoding="utf-8", errors="replace") as f:
        text = f.read()
    lines = text.splitlines()
    threads = []
    cur = None
    for ln in lines:
        if ln.startswith('"'):
            m = re.match(r'^"(.*?)"\s', ln)
            name = m.group(1) if m else ln[:80]
            cur = {"name": name, "header": ln, "state": None, "frames": [],
                   "locks": [], "daemon": "daemon" in ln}
            threads.append(cur)
            continue
        if cur is None:
            continue
        s = ln.strip()
        if s.startswith("java.lang.Thread.State:"):
            cur["state"] = s.split(":", 1)[1].strip()
        elif s.startswith("at ") and len(cur["frames"]) < 200:
            cur["frames"].append(s[3:])
        elif "Locked ownable synchronizers" in s or s.startswith("- waiting to lock") or s.startswith("- locked"):
            cur["locks"].append(s)
    return threads, text


def short(frame):
    return frame


def report_threads(path, label):
    threads, text = parse_thread_dump(path)
    print(f"\n########## {label} ##########")
    print(f"file={path}")
    print(f"threads={len(threads)} size={len(text)/1e6:.2f}MB")
    print("state histogram:", dict(Counter(t["state"] for t in threads)))

    # 关键线程
    keys = ("AWT-EventQueue", "AWT-Windows", "ApplicationImpl pooled thread",
            "JobScheduler", "Jobs", "Index", "Cangjie", "cangjie", "LSP", "lsp",
            "Alarm", "ForkJoinPool", "Cached thread pool", "WriteAction")
    interesting = [t for t in threads if any(k.lower() in t["name"].lower() for k in keys)]
    print(f"interesting threads={len(interesting)}")
    for t in interesting[:60]:
        print(f"\n-- {t['header'][:160]}")
        print(f"   state={t['state']}")
        for fr in t["frames"][:18]:
            print("     at", fr)
        for lk in t["locks"][:3]:
            print("   ", lk)

    # 未匹配但在 RUNNABLE 且有栈的线程（可能是热点）
    print("\n=== RUNNABLE threads (top 25 by first frame) ===")
    runnable = [t for t in threads if t["state"] == "RUNNABLE" and t["frames"]]
    for t in runnable[:25]:
        print(f"[{t['name'][:60]}] {t['frames'][0][:150]}")
    return threads


def analyze_log(path):
    print(f"\n########## idea.log ##########\n{path}")
    with io.open(path, "r", encoding="utf-8", errors="replace") as f:
        lines = f.read().splitlines()
    print("lines:", len(lines))
    lvl = Counter()
    entries = []
    pat = re.compile(r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})\s+\[\s*(\d+)\]\s+(\w+) - (.+)$")
    for ln in lines:
        m = pat.match(ln)
        if not m:
            continue
        ts, off, level, rest = m.groups()
        lvl[level] += 1
        entries.append((ts, int(off), level, rest))
    print("level counts:", dict(lvl))

    def sig(msg):
        s = re.sub(r"\d+", "#", msg)
        s = re.sub(r"[0-9a-f]{8,}", "<hex>", s)
        return s[:180]

    for level in ("ERROR", "WARN"):
        print(f"\n=== {level} grouped (top 30) ===")
        c = Counter(sig(e[3]) for e in entries if e[2] == level)
        for s, n in c.most_common(30):
            print(f"{n:6d}  {s}")

    print("\n=== ERROR samples (first 40, deduped) ===")
    seen = set()
    for ts, off, level, rest in entries:
        if level != "ERROR":
            continue
        s = sig(rest)
        if s in seen:
            continue
        seen.add(s)
        print(f"{ts} {rest[:400]}")
        if len(seen) >= 40:
            break

    # 抑制/慢操作/内存
    patterns = {
        "GC/low-memory": r"LowMemory|gc overloaded|Memory deficit",
        "freeze": r"frozen|freeze",
        "indexing": r"[Ii]ndex",
        "outofmemory": r"OutOfMemory|OOM",
        "slow-op": r"Slow operations|took \d+ ms|in \d{4,} ms",
        "write-action": r"background write action",
        "plugin": r"PluginManager|plugin",
        "cancel": r"CancellationException|was cancelled",
    }
    for name, rx in patterns.items():
        hits = [e for e in entries if re.search(rx, e[3])]
        print(f"\n--- {name}: {len(hits)} hits")
        c = Counter(sig(e[3]) for e in hits)
        for s, n in c.most_common(8):
            print(f"  {n:5d}  {s}")
        for e in hits[:3]:
            print(f"   first@{e[0]} {e[3][:240]}")


def analyze_metrics(directory):
    files = [f for f in os.listdir(directory) if f.startswith("open-telemetry-metrics.") and f.endswith(".csv")]
    if not files:
        print("no metrics csv")
        return
    files.sort()
    path = os.path.join(directory, files[-1])
    print(f"\n########## metrics ##########\n{path}")
    rows = defaultdict(list)
    with io.open(path, "r", encoding="utf-8", errors="replace") as f:
        for ln in f:
            if not ln or ln.startswith("#"):
                continue
            parts = ln.rstrip("\n").split(",")
            if len(parts) < 4:
                continue
            name, start, end, val = parts[0], parts[1], parts[2], parts[3]
            try:
                rows[name].append((int(end), float(val)))
            except ValueError:
                pass
    want = re.compile(r"memory|heap|gc|Memory|Gc|GC|threadCount|ThreadCount|classLoading|NonHeap")
    for name in sorted(rows):
        if not want.search(name):
            continue
        vals = rows[name]
        latest = vals[-1][1]
        mn = min(v for _, v in vals)
        mx = max(v for _, v in vals)
        print(f"{name:60s} latest={latest:>16.0f} min={mn:>16.0f} max={mx:>16.0f} n={len(vals)}")


if __name__ == "__main__":
    freeze = os.path.join(LOG, "threadDumps-freeze-20261022-124946-IU-253.29346.50-Unsafe.park-42sec",
                          "threadDump-20261022-125004.txt")
    report_threads(freeze, "FREEZE 42s snapshot 12:50:04")
    bg = os.path.join(LOG, "bg-wa", "thread-dump-205654388.txt")
    if os.path.exists(bg):
        report_threads(bg, "BACKGROUND WRITE ACTION STUCK 12:50:10")
    analyze_log(os.path.join(LOG, "idea.log"))
    analyze_metrics(LOG)
