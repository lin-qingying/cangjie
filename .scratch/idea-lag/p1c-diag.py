import io, re
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
idx = [i for i, l in enumerate(lines) if "diagnostic collector has been interrupted" in l.lower()]
print("hits:", len(idx))
if idx:
    i = idx[0]
    for x in lines[max(0, i-6):i+30]:
        print(x[:260])
