import io
import re
from collections import Counter

log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
CUT = "2026-10-22 2026-10-23 18:28:00"
pat = re.compile(r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})\s+\[\s*(\d+)\]\s+(\w+) - (.+)$")
entries = []
with io.open(log, encoding="utf-8", errors="replace") as f:
    for ln in f:
        m = pat.match(ln.rstrip("\n"))
        if m and m.group(1) >= CUT:
            entries.append((m.group(1), m.group(3), m.group(4)))
print("entries:", len(entries), "levels:", dict(Counter(e[1] for e in entries)))
for name, rx in [
    ("freeze", r"UI was frozen"),
    ("lowmem", r"Low memory signal"),
    ("highmem", r"High memory usage"),
    ("stub-count", r"Stub count"),
    ("bg-wa", r"Cannot execute background write action"),
    ("macro-failed", r"Failed to construct the macro semantic snapshot"),
    ("cdoc", r"has no CJC TokenKind mapping"),
    ("published", r"Published macro semantic snapshot"),
    ("reused", r"Reused macro semantic snapshot"),
    ("failed-skip", r"previously failed"),
    ("diag-interrupted", r"The diagnostic collector has been interrupted"),
    ("ref-unresolved", r"Unable to resolve reference class"),
    ("cache-inconsistency", r"Inconsistency in the cache"),
]:
    hits = [e for e in entries if re.search(rx, e[2])]
    print(f"{name:18s} {len(hits):4d}  first={hits[0][0] if hits else '-'} last={hits[-1][0] if hits else '-'}")

print("\n=== SEVERE 摘要（去环境头） ===")
hdr = re.compile(r"IntelliJ IDEA|JDK:|Vendor:|OS: Windows|Plugin to blame|Last Action")
sev = [e for e in entries if e[1] == "SEVERE" and not hdr.search(e[2])]
for logger, n in Counter(e[2].split(" - ")[0] for e in sev).most_common(8):
    print(f"{n:4d} {logger}")

print("\n=== 宏相关日志 ===")
for ts, lvl, rest in entries:
    if "macro semantic snapshot" in rest.lower():
        print(f"{ts} {lvl} {rest[:200]}")

