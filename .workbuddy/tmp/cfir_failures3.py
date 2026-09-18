import json, collections, re

SNAP = r"D:\code\intellij\cangjie\cfir\analysis-tests\build\ffi-annotation-verification\20260918-full-after-platform-annotation\cases.json"
with open(SNAP, encoding="utf-8") as f:
    cases = json.load(f)
fails = [c for c in cases if c.get("status") == "failed"]

def parse(key):
    cls, _, meth = key.partition("#")
    short = cls.split(".")[-1]
    parts = short.split("$")
    base = parts[0].replace("Psi", "")
    return base, "$".join(parts[1:]), meth.replace("()", "")

norm = {}
for c in fails:
    base, path, meth = parse(c["key"])
    norm.setdefault((base, path, meth), c)

# extract "expected"/"actual" blocks
diag_re = re.compile(r"\b([A-Z][A-Z0-9_]{4,})\b")

def mk_family(msg):
    # look for concrete diagnostic names mentioned
    names = set(diag_re.findall(msg))
    names = {n for n in names if "_" in n or n.endswith("ERROR")}
    return tuple(sorted(names))[:6]

fam = collections.Counter()
examples = {}
for (base, path, meth), c in norm.items():
    f = mk_family(c["message"])
    fam[f] += 1
    examples.setdefault(f, (base, path, meth))

print("== distinct failure signatures (diagnostic-name sets) ==")
for f, n in fam.most_common(40):
    b, p, m = examples[f]
    print(f"\n--- {n} case(s) --- e.g. {b} : {p} #{m}")
    print("   names:", ", ".join(f) if f else "(none)")
