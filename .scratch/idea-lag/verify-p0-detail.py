import io
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
for i, ln in enumerate(lines):
    if "20:31:14,052" in ln and "Failed to construct the macro semantic snapshot" in ln:
        print("=== macro failure block ===")
        for x in lines[i:i+22]:
            print(x[:240])
        break
for i, ln in enumerate(lines):
    if "20:3" in ln and "SEVERE" in ln and "#c.i.u.Alarm" in ln:
        print("\n=== Alarm SEVERE ===")
        for x in lines[i:i+12]:
            print(x[:240])
        break
