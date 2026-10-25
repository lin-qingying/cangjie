import io
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
for l in lines:
    if l[:16] >= "2026-10-23 18:42" and l[:16] <= "2026-10-23 18:43":
        print(l[:200])
