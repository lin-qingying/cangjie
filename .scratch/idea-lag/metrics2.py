import io, os, re
from collections import defaultdict
p = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\open-telemetry-metrics.2026-10-22-12-43-03.csv"
rows = defaultdict(list)
for ln in io.open(p, encoding="utf-8", errors="replace"):
    if not ln or ln.startswith("#"): continue
    parts = ln.rstrip("\n").split(",")
    if len(parts) < 4: continue
    try: rows[parts[0]].append(float(parts[3]))
    except ValueError: pass
keys = sorted(k for k in rows if re.search(r"cangjie|Cangjie|macro|psi|Psi|parse|Parse|analysis|Analysis", k))
print("matched", len(keys))
for k in keys[:40]:
    v = rows[k]
    print(f"{k:70s} n={len(v):3d} last={v[-1]:>14.0f} max={max(v):>14.0f}")
