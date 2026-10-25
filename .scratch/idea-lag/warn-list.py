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
hdr = re.compile(r"IntelliJ IDEA|JDK:|Vendor:|OS: Windows|Plugin to blame|Last Action")
print("=== 所有 WARN / SEVERE（非环境头）===")
for ts, lvl, msg in rows:
    if lvl in ("WARN","SEVERE") and not hdr.search(msg):
        print(f"{ts[11:19]} {lvl:6s} {msg[:190]}")
print("\n=== 含 cangjie/cangnova 的时间戳行 ===")
for ts, lvl, msg in rows:
    if "cangjie" in msg.lower() or "cangnova" in msg:
        print(f"{ts[11:19]} {lvl:6s} {msg[:190]}")
