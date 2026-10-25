import io, re
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
ts = re.compile(r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})")
for i, l in enumerate(lines):
    if "Expected CfirNamedReference or CfirErrorNamedReference" in l:
        j = i
        while j >= 0 and not ts.match(lines[j]):
            j -= 1
        print("hit line", i+1, "owner timestamp:", ts.match(lines[j]).group(1))
        print("  header:", lines[j][:200])
