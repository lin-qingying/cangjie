import io, re
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
CUT = "2026-10-23 20:13:00"
pat = re.compile(r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})\s+\[\s*(\d+)\]\s+(\w+) - (.+)$")
rows = []
for ln in lines:
    m = pat.match(ln)
    if m and m.group(1) >= CUT:
        rows.append((m.group(1), m.group(3), m.group(4)))
print("entries:", len(rows))
for name, rx in [
    ("freeze", r"UI was frozen"), ("lowmem", r"Low memory signal"), ("highmem", r"High memory usage"),
    ("stub-count", r"Stub count"), ("bg-wa", r"Cannot execute background write action"),
    ("macro-failed", r"Failed to construct the macro semantic snapshot"), ("published", r"Published macro semantic snapshot"),
    ("diag-interrupted", r"The diagnostic collector has been interrupted"),
    ("ref-unresolved", r"Unable to resolve reference class"), ("cache-inconsistency", r"Inconsistency in the cache"),
]:
    hits = [e for e in rows if re.search(rx, e[2])]
    print(f"{name:18s} {len(hits):3d}  first={hits[0][0] if hits else '-'}")
sev = [e for e in rows if e[1] == "SEVERE" and not re.search(r"IntelliJ IDEA|JDK:|Vendor:|OS: Windows|Plugin to blame", e[2])]
print("SEVERE:", len(sev))
for e in sev[:5]:
    print(" ", e[0][11:19], e[2][:120])


