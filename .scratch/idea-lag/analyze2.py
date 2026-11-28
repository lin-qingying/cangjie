"""更细的分析：SEVERE 日志、BLOCKED 线程、内存指标时间线。"""
import io
import os
import re
from collections import Counter, defaultdict

SANDBOX = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1"
LOG = os.path.join(SANDBOX, "log_runIde")
FREEZE = os.path.join(LOG, "threadDumps-freeze-20261022-124946-IU-253.29346.50-Unsafe.park-42sec",
                      "threadDump-20261022-125004.txt")


def norm(s):
    s = re.sub(r"\d+", "#", s)
    s = re.sub(r"0x[0-9a-f]+", "<addr>", s)
    s = re.sub(r"[0-9a-f]{8,}", "<hex>", s)
    return s[:220]


def severe():
    path = os.path.join(LOG, "idea.log")
    entries = []
    pat = re.compile(r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})\s+\[\s*(\d+)\]\s+(\w+) - (.+)$")
    with io.open(path, "r", encoding="utf-8", errors="replace") as f:
        cur = None
        for ln in f:
            ln = ln.rstrip("\n")
            m = pat.match(ln)
            if m:
                cur = [m.group(1), m.group(2), m.group(3), m.group(4)]
                entries.append(cur)
            elif cur is not None and (ln.startswith("\tat ") or ln.startswith("Caused by:")
                                       or ln.startswith("\t...") or "Exception" in ln[:60]):
                cur[3] += " | " + ln.strip()
    sev = [e for e in entries if e[2] in ("SEVERE", "ERROR")]
    print(f"SEVERE/ERROR entries: {len(sev)}")
    c = Counter(norm(e[3]) for e in sev)
    for s, n in c.most_common(25):
        print(f"{n:5d}  {s}")
    print("\n=== distinct cangjie-related severe ===")
    c2 = Counter(norm(e[3]) for e in sev if "cangjie" in e[3].lower() or "cangnova" in e[3].lower())
    for s, n in c2.most_common(20):
        print(f"{n:5d}  {s}")
    print("\n=== first 8 severe with time ===")
    for e in sev[:8]:
        print(f"{e[0]} {e[3][:300]}")


def blocked_threads():
    with io.open(FREEZE, "r", encoding="utf-8", errors="replace") as f:
        lines = f.read().splitlines()
    blocks, cur = [], None
    for ln in lines:
        if ln.startswith('"'):
            cur = {"name": ln.split('"')[1], "frames": [], "state": None}
            blocks.append(cur)
        elif cur is not None:
            s = ln.strip()
            if s.startswith("java.lang.Thread.State:"):
                cur["state"] = s.split(":", 1)[1].strip()
            elif s.startswith("at "):
                cur["frames"].append(s[3:])
    blocked = [b for b in blocks if b["state"] == "BLOCKED"]
    print(f"\nBLOCKED threads: {len(blocked)}")
    c = Counter(b["frames"][1] if len(b["frames"]) > 1 else "?" for b in blocked)
    for s, n in c.most_common(10):
        print(f"{n:3d}  {s}")
    print("\n--- sample ---")
    for b in blocked[:3]:
        print(f"\n[{b['name']}]")
        for fr in b["frames"][:9]:
            print("   at", fr)

    # 所有线程里 cangjie 相关帧的分布
    print("\n=== 出现 cangnova/cangjie 帧的线程数 ===")
    hit = [b for b in blocks if any("cangnova" in f.lower() for f in b["frames"])]
    print(f"{len(hit)} / {len(blocks)}")
    for b in hit[:12]:
        top = next(f for f in b["frames"] if "cangnova" in f.lower())
        print(f"[{b['state']:<14}] {b['name'][:38]:38s} -> {top[:110]}")


def metrics():
    files = sorted(f for f in os.listdir(LOG) if f.startswith("open-telemetry-metrics.") and f.endswith(".csv"))
    path = os.path.join(LOG, files[-1])
    print(f"\n=== metrics {files[-1]} ===")
    rows = defaultdict(list)
    with io.open(path, "r", encoding="utf-8", errors="replace") as f:
        for ln in f:
            if not ln or ln.startswith("#"):
                continue
            p = ln.rstrip("\n").split(",")
            if len(p) < 4:
                continue
            try:
                rows[p[0]].append((int(p[2]) / 1e9, float(p[3])))
            except ValueError:
                pass
    for name in sorted(rows):
        if re.search(r"jvm\.(memory|gc)|memory\.|gc\.", name, re.I):
            vals = rows[name]
            print(f"{name:55s} n={len(vals):3d} min={min(v for _, v in vals):>15.0f} max={max(v for _, v in vals):>15.0f} last={vals[-1][1]:>15.0f}")
    # 展示 heap used 时间序列
    for key in ("jvm.memory.used", "jvm.memory.committed", "jvm.memory.max"):
        cand = [k for k in rows if k.endswith(key)]
        for k in cand:
            print(f"\n--- {k} series (last 15) ---")
            for ts, v in rows[k][-15:]:
                print(f"  t={ts:.0f} value={v/1e6:.0f} MB")


if __name__ == "__main__":
    severe()
    blocked_threads()
    metrics()
