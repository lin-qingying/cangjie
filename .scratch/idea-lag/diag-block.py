import io, re
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
# find first diagnostic collector WARN after 20:09
start = None
for i, ln in enumerate(lines):
    if "The diagnostic collector has been interrupted" in ln and " 20:1" in ln:
        start = i
        break
print("start line", start+1 if start is not None else None)
if start is not None:
    for ln in lines[start:start+70]:
        print(ln[:260])
