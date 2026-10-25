import io, re, os
from collections import Counter
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
CUT = "2026-10-22 20:09:00"
pat = re.compile(r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})\s+\[\s*(\d+)\]\s+(\w+) - (.+)$")
entries = []
with io.open(log, encoding="utf-8", errors="replace") as f:
    for ln in f:
        m = pat.match(ln.rstrip("\n"))
        if m and m.group(1) >= CUT:
            entries.append((m.group(1), m.group(3), m.group(4)))
print("entries since 20:09:", len(entries))
print("levels:", dict(Counter(e[1] for e in entries)))

def norm(s):
    s = re.sub(r"\d+", "#", s)
    s = re.sub(r"0x[0-9a-f]+", "<addr>", s)
    return s[:200]

for level in ("SEVERE", "WARN"):
    print(f"\n=== {level} top signatures ===")
    for s, n in Counter(norm(e[2]) for e in entries if e[1] == level).most_common(15):
        print(f"{n:4d}  {s}")

keys = {
    "stub-count": r"Stub count",
    "freeze": r"UI was frozen",
    "lowmem": r"Low memory signal",
    "highmem": r"High memory usage",
    "bg-wa": r"Cannot execute background write action",
    "rebuild": r"Stub index rebuild requested",
    "published": r"Published macro semantic snapshot",
    "reused": r"Reused macro semantic snapshot",
    "input-changed": r"input changed",
}
print("\n=== key events (this session) ===")
for name, rx in keys.items():
    hits = [e for e in entries if re.search(rx, e[2])]
    print(f"{name:14s} count={len(hits):3d} first={hits[0][0] if hits else '-'} last={hits[-1][0] if hits else '-'}")

print("\n=== SEVERE samples ===")
seen = set()
for ts, lvl, rest in entries:
    if lvl != "SEVERE": continue
    s = norm(rest)
    if s in seen: continue
    seen.add(s)
    print(f"{ts} {rest[:300]}")
    if len(seen) >= 12: break
