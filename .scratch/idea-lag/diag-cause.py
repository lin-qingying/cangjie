import io
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
for i, l in enumerate(lines):
    if "20:17:12,633" in l and "diagnostic collector" in l:
        print(l[:200])
        for x in lines[i:i+100]:
            if "Caused by" in x:
                print("  ", x[:230])
        break
