import io
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
idx = [i for i, l in enumerate(lines) if "diagnostic collector has been interrupted" in l.lower()]
i = idx[0]
block = lines[i:i+120]
for x in block:
    if "Caused by" in x or "expected" in x or "ResolvedTo" in x or "checkPhase" in x:
        print(x[:260])
print("---- around end of first block ----")
for x in block[30:70]:
    print(x[:200])
