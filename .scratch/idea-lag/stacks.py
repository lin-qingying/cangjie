import io
import os
import re

FREEZE = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\threadDumps-freeze-20261022-124946-IU-253.29346.50-Unsafe.park-42sec\threadDump-20261022-125004.txt"

blocks, cur = [], None
with io.open(FREEZE, "r", encoding="utf-8", errors="replace") as f:
    for ln in f:
        ln = ln.rstrip("\n")
        if ln.startswith('"'):
            cur = {"name": ln.split('"')[1], "header": ln, "frames": [], "state": None}
            blocks.append(cur)
        elif cur is not None:
            s = ln.strip()
            if s.startswith("java.lang.Thread.State:"):
                cur["state"] = s.split(":", 1)[1].strip()
            elif s.startswith("at "):
                cur["frames"].append(s[3:])


def show(name, limit=60):
    for b in blocks:
        if b["name"] == name:
            print(f"\n===== {b['header'][:150]}")
            print(f"state={b['state']} frames={len(b['frames'])}")
            for fr in b["frames"][:limit]:
                print("  at", fr)
            return
    print("not found:", name)


show("AWT-EventQueue-0", 80)
show("ApplicationImpl pooled thread 1", 60)
show("ApplicationImpl pooled thread 63", 60)

print("\n\n===== 含 WriteAction 帧的线程 =====")
for b in blocks:
    if any("WriteAction" in f or "runWriteAction" in f for f in b["frames"]):
        print(f"[{b['state']}] {b['name']}")
        for fr in b["frames"][:15]:
            print("   at", fr)
        print()

print("\n===== 线程名前缀统计 =====")
from collections import Counter
c = Counter(re.sub(r"\s*\d+$", "", b["name"]) for b in blocks)
for k, v in c.most_common(20):
    print(f"{v:4d}  {k}")
