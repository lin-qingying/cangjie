import io
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()

def dump(marker, limit=3, after=30):
    shown = 0
    for i, ln in enumerate(lines):
        if marker in ln and " 20:" in ln:
            print(f"\n===== {marker} @ line {i+1} =====")
            for j in range(i, min(i + after, len(lines))):
                print(lines[j][:300])
            shown += 1
            if shown >= limit:
                return

dump("Unable to resolve reference class")
dump("Inconsistency in the cache")
