import glob, os, re
from collections import defaultdict

ROOT = r"D:\code\intellij\cangjie\analysis\stubs\testData\builtins"
# Capture annotation lines and member declaration lines separately per extend block.
member_re = re.compile(r"^\s*(public|private|protected|internal|static|open|override|operator|redef|abstract|common|specific|default|const|mut|unsafe|foreign)?[a-z ]*\b(func|prop|init|var|let)\b\s*([^\s(<:={]*)")

for path in sorted(glob.glob(os.path.join(ROOT, "*.cjo.decompiled.text.cj"))):
    pkg = os.path.basename(path).replace(".cjo.decompiled.text.cj", "")
    cur = None
    pending = []
    for lineno, raw in enumerate(open(path, encoding="utf-8", errors="replace"), 1):
        line = raw.rstrip("\n")
        s = line.strip()
        if line.startswith("extend "):
            cur = (lineno, line, [])
            pending = []
            continue
        if cur is None:
            continue
        if line.startswith("}"):
            # group by (kind,name), report differing mod/annotation sets
            groups = defaultdict(list)
            for item in cur[2]:
                groups[item[0]].append(item)
            for k, items in groups.items():
                sigs = {tuple(sorted(it[1])) for it in items}
                if len(items) > 1 and len(sigs) > 1:
                    print("%s:%s %s" % (pkg, cur[0], cur[1][:80]))
                    for it in items:
                        print("    line %s %-30s %s" % (it[2], k, " ".join(it[1])))
            cur = None
            continue
        if s.startswith("@"):
            pending.append(s)
            continue
        m = member_re.match(line)
        if m:
            kind = m.group(2)
            name = m.group(3)
            # normalize modifier words
            mods = [w for w in ("public","private","protected","internal","static","open","override","operator","redef","abstract") if re.search(r"\b%s\b" % w, line)]
            annots = [a for a in pending if not a.startswith("@Frozen")]
            cur[2].append((kind + "|" + name, mods + annots, lineno))
            pending = []
        else:
            if s:
                pending = []
