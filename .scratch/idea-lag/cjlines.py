import io
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
sel = [l for l in lines if l[:19] >= "2026-10-23 18:42:10" and ("cangnova" in l or "CangJie" in l or "cangjie" in l)]
print("cangjie lines:", len(sel))
for l in sel[-12:]:
    print(l[:220])
