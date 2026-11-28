import io, re
from collections import Counter
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
CUT = "2026-10-22 20:09:00"
pat = re.compile(r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}):\d{2},\d{3}\s+\[\s*\d+\]\s+(\w+) - (.+)$")
rows = []
for ln in io.open(log, encoding="utf-8", errors="replace"):
    m = pat.match(ln.rstrip("\n"))
    if m and m.group(0)[:19] >= CUT:
        rows.append((m.group(1), m.group(2), m.group(3)))

def fam(rx):
    hits = [r for r in rows if re.search(rx, r[2])]
    per_min = Counter(r[0] for r in hits)
    return hits, per_min

for name, rx in [
    ("ref-unresolved", r"Unable to resolve reference class"),
    ("diag-interrupted", r"The diagnostic collector has been interrupted"),
    ("cache-inconsistency", r"Inconsistency in the cache"),
    ("phase-contract", r"must not be null due to the contract|At least .* expected but ResolvedTo"),
]:
    hits, per_min = fam(rx)
    print(f"\n### {name}: {len(hits)} hits")
    if hits:
        print("first:", hits[0][0], "last:", hits[-1][0])
        print("per-minute:", dict(sorted(per_min.items())))

print("\n### SEVERE by logger (excl. header noise)")
hdr = re.compile(r"IntelliJ IDEA|JDK:|Vendor:|OS: Windows|Plugin to blame|Last Action")
sev = [r for r in rows if r[1] == "SEVERE" and not hdr.search(r[2])]
for logger, n in Counter(r[2].split(" - ")[0] for r in sev).most_common(10):
    print(f"{n:4d}  {logger}")
