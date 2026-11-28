import io
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
for i, l in enumerate(lines):
    if "20:02:44,231" in l and "diagnostic collector" in l:
        for x in lines[i:i+60]:
            if "Caused by" in x or "Error while resolving" in x or "must not be null" in x:
                print(x[:240])
        break
print("--- cache inconsistency owners ---")
for i, l in enumerate(lines):
    if "20:02:2" in l and "Inconsistency in the cache" in l:
        print(l[:200])
        for x in lines[i+1:i+6]: print("   ", x[:200])
