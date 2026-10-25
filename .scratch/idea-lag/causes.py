import io, re
from collections import Counter
log = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\idea.log"
lines = io.open(log, encoding="utf-8", errors="replace").read().splitlines()
CUT = "2026-10-22 20:09:00"
idx = [i for i, l in enumerate(lines) if l[:19] >= CUT]
print("lines in session:", len(idx))

def blocks(marker, maxn=3, span=140):
    out = []
    for i in idx:
        if marker in lines[i]:
            buf = []
            depth = 0
            for j in range(i, min(i + span, len(lines))):
                buf.append(lines[j])
                if lines[j].startswith("Caused by:") or lines[j].strip().startswith("Suppressed:"):
                    depth += 1
            out.append((i, buf))
            if len(out) >= maxn:
                break
    return out

for marker in ["Unable to resolve reference class", "The diagnostic collector has been interrupted"]:
    print(f"\n########## {marker} ##########")
    for i, buf in blocks(marker, maxn=2):
        print(f"--- line {i+1} ---")
        for ln in buf:
            if ln.startswith("Caused by") or "Suppressed:" in ln or "Exception" in ln or "Error while" in ln or "expected" in ln:
                print(ln[:260])
