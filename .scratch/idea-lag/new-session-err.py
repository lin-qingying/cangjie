import io
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
idx = [i for i, l in enumerate(lines) if "19:51:3" in l and ("diagnostic collector has been interrupted" in l or "Inconsistency in the cache" in l)]
for i in idx:
    print("="*20)
    for x in lines[i:i+8]:
        print(x[:230])
