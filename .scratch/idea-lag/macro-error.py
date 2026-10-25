import io
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
target = None
for i, ln in enumerate(lines):
    if "20:12:18,269" in ln and "LLCfirMacroSessionConfigurator" in ln:
        target = i
        break
print("target line", target+1 if target else None)
if target:
    for ln in lines[max(0, target-30):target+6]:
        print(ln[:280])
