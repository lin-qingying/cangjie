import io
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
starts = [i for i, l in enumerate(lines) if "20:17:12,633" in l and "diagnostic collector" in l]
i = starts[0]
end = i + 1
while end < len(lines) and not lines[end][:4] == "2026":
    end += 1
for x in lines[i:end]:
    if "Caused by" in x or "NullPointer" in x or "must not be null" in x:
        print(x[:240])
