import glob, os, re

ROOT = r"D:\code\intellij\cangjie\analysis\stubs\testData\builtins"
for path in sorted(glob.glob(os.path.join(ROOT, "*.cjo.decompiled.text.cj"))):
    pkg = os.path.basename(path).replace(".cjo.decompiled.text.cj", "")
    cur = None
    for lineno, raw in enumerate(open(path, encoding="utf-8", errors="replace"), 1):
        line = raw.rstrip("\n")
        if line.startswith("extend "):
            cur = [lineno, line, {}]
            continue
        if cur is None:
            continue
        if line.startswith("}"):
            dups = {k: v for k, v in cur[2].items() if v > 1}
            if dups:
                print("%s:%s %s  DUP=%s" % (pkg, cur[0], cur[1][:90], dups))
            cur = None
            continue
        m = re.search(r"\b(func|prop|init|var|let)\b\s*([^\s(<:={]*)", line)
        if m:
            k = m.group(1) + "|" + m.group(2)
            cur[2][k] = cur[2].get(k, 0) + 1
