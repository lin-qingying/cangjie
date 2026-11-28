import io
import re
from collections import Counter

log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
CUT = "2026-10-23 18:41:30"
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
    ("token-unmapped", r"has no CJC TokenKind mapping"),
    ("published", r"Published macro semantic snapshot"),
    ("capture-canceled", r"input capture was canceled"),
    ("diag-interrupted", r"The diagnostic collector has been interrupted"),
    ("ref-unresolved", r"Unable to resolve reference class"),
    ("cache-inconsistency", r"Inconsistency in the cache"),
]:
    hits = [e for e in entries if re.search(rx, e[2])]
    print(f"{name:18s} {len(hits):4d}  first={hits[0][0] if hits else '-'} last={hits[-1][0] if hits else '-'}")

print("\n=== SEVERE（去环境头） ===")
hdr = re.compile(r"IntelliJ IDEA|JDK:|Vendor:|OS: Windows|Plugin to blame|Last Action")
sev = [e for e in entries if e[1] == "SEVERE" and not hdr.search(e[2])]
for logger, n in Counter(e[2].split(" - ")[0] for e in sev).most_common(8):
    print(f"{n:4d} {logger}")

print("\n=== 18:28 之后的宏与错误详情 ===")
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
for i, ln in enumerate(lines):
    if "18:28:53" in ln and "Failed to construct" in ln:
        for x in lines[i:i+5]:
            print(x[:240])
        break
for i, ln in enumerate(lines):
    if "18:28:47" in ln and "Low memory" in ln:
        print("\nLOWMEM:", ln[:240])
        break

