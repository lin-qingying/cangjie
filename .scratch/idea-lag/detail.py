import io, re
from collections import Counter
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
pat = re.compile(r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})\s+\[\s*(\d+)\]\s+(\w+) - (.+)$")
cut = "2026-10-23 18:41:30"
rows = []
for ln in io.open(log, encoding="utf-8", errors="replace"):
    m = pat.match(ln.rstrip("\n"))
    if m and m.group(1) >= cut:
        rows.append((m.group(1), m.group(3), m.group(4)))
print("\n=== WARN 按 logger ===")
for lg, n in Counter(r[2].split(" - ")[0] for r in rows if r[1] == "WARN").most_common(10):
    print(f"{n:4d} {lg}")
print("\n=== 最近 25 条（任意级别，去统计噪声） ===")
noise = re.compile(r"EventLogStatisticsService|Statistics\.|ProvenanceDatabaseService|GistStorageImpl|CodeWithMeCleanup|MarketplaceRequests")
recent = [r for r in rows if not noise.search(r[2])]
for ts, lvl, msg in recent[-25:]:
    print(f"{ts[11:19]} {lvl:6s} {msg[:150]}")
print("\ntotal:", len(rows), "noise-filtered:", len(recent))
