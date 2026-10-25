import io, re
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
ts = re.compile(r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})")
cut = "2026-10-23 18:42:10"
print("log last line:", lines[-1][:160] if lines else "-")
cnt = 0
for l in lines:
    m = ts.match(l)
    if not m or m.group(1) < cut: continue
    if "cangjie" in l.lower() or "cangnova" in l:
        cnt += 1
        if cnt <= 8: print(l[:230])
print("cangjie timestamped lines in session:", cnt)
