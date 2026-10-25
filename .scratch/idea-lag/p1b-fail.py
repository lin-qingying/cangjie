import io
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
for i, ln in enumerate(lines):
    if "21:51:10,321" in ln and "Failed to construct" in ln:
        print("=== macro failure ===")
        for x in lines[i:i+6]: print(x[:240])
    if "has no CJC TokenKind mapping" in ln and "21:5" in ln:
        print("\n=== cdoc ===")
        for x in lines[i:i+6]: print(x[:240])
