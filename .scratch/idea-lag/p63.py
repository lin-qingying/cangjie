import io
F = r"D:\code\intellij\cangjie\intellij-ide\.intellijPlatform\sandbox\intellij-cangjie\IU-2025.3.1\log_runIde\threadDumps-freeze-20261022-124946-IU-253.29346.50-Unsafe.park-42sec\threadDump-20261022-125004.txt"
blocks, cur = [], None
for ln in io.open(F, encoding="utf-8", errors="replace"):
    ln = ln.rstrip("\n")
    if ln.startswith('"'):
        cur = {"name": ln.split('"')[1], "frames": []}
        blocks.append(cur)
    elif cur is not None and ln.strip().startswith("at "):
        cur["frames"].append(ln.strip()[3:])
for b in blocks:
    if b["name"] == "ApplicationImpl pooled thread 63":
        for i, fr in enumerate(b["frames"]):
            print(f"{i:3d} {fr}")
