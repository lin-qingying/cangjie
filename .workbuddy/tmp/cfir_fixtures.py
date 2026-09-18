import json, os, re, collections

SNAP = r"D:\code\intellij\cangjie\cfir\analysis-tests\build\ffi-annotation-verification\20260918-full-after-platform-annotation\cases.json"
GEN = r"D:\code\intellij\cangjie\cfir\analysis-tests\tests-gen"

with open(SNAP, encoding="utf-8") as f:
    cases = json.load(f)
fails = [c for c in cases if c.get("status") == "failed"]

def parse(key):
    cls, _, meth = key.partition("#")
    short = cls.split(".")[-1]
    parts = short.split("$")
    base = parts[0].replace("Psi", "")
    return base, "$".join(parts[1:]), meth.replace("()", "")

# build index: (file-stem, path, method) -> fixture
idx = {}
pat = re.compile(r'fun\s+(test\w*)\s*\([^)]*\)\s*\{\s*\n\s*runTest\("([^"]+)"')
for dirpath, _, files in os.walk(GEN):
  for fn in files:
    if not fn.endswith(".kt"):
        continue
    stem = fn[:-3].replace("Psi", "")
    text = open(os.path.join(dirpath, fn), encoding="utf-8", errors="replace").read()
    for m in pat.finditer(text):
        idx[(stem, m.group(1))] = m.group(2)

# also derive path from nearest class declaration -> use simple approach: map method->fixture (unique enough)
meth_map = {}
for (stem, meth), path in idx.items():
    meth_map.setdefault(meth, set()).add(path)

rows = []
for c in fails:
    base, path, meth = parse(c["key"])
    fixtures = meth_map.get(meth, {None})
    rows.append((base, path, meth, sorted(x for x in fixtures if x)))

uniq = collections.OrderedDict()
for base, path, meth, fixtures in rows:
    key = (meth, tuple(fixtures))
    uniq.setdefault(key, {"base": base, "path": path, "paths": fixtures})

print("distinct failing fixtures:", len(uniq))
for (meth, _), v in uniq.items():
    print(f"{v['base']:32s} {v['path']:55s} {meth:45s} -> {', '.join(v['paths']) if v['paths'] else '?'}")
