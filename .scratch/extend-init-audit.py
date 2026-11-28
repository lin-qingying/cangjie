import glob, os, re

ROOT = r"D:\code\intellij\cangjie\analysis\stubs\testData\builtins"
for path in sorted(glob.glob(os.path.join(ROOT, "*.cjo.decompiled.text.cj"))):
    pkg = os.path.basename(path).replace(".cjo.decompiled.text.cj", "")
    cur = None
    for lineno, raw in enumerate(open(path, encoding="utf-8", errors="replace"), 1):
        line = raw.rstrip("\n")
        if line.startswith("extend "):
            cur = [lineno, line, []]
            continue
        if cur is None:
            continue
        if line.startswith("}"):
            if cur[2]:
                print("%s:%s %s -> %s" % (pkg, cur[0], cur[1][:70], cur[2]))
            cur = None
            continue
        if re.search(r"\binit\s*\(", line):
            cur[2].append(line.strip())
