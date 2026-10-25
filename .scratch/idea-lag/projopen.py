import io
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
for ln in io.open(log, encoding="utf-8", errors="replace"):
    if "18:3" in ln and ("icu4cj" in ln or "Project" in ln and "open" in ln.lower()):
        print(ln[:220].rstrip())
