import io
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
idx = [i for i, l in enumerate(lines) if "Failed to construct the macro semantic snapshot" in l]
for i in idx:
    print("="*30)
    for ln in lines[i:i+8]:
        print(ln[:240])
