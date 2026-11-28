import io
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
idx = None
for i, ln in enumerate(lines):
    if "MacroForestEvaluator.evaluate(MacroCallForest.kt:441)" in ln and " 20:12:" in ln:
        idx = i
        break
print("anchor", idx+1 if idx else None)
if idx:
    for ln in lines[max(0, idx-45):idx+3]:
        print(ln[:280])
