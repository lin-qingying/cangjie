import json, collections, re

SNAP = r"D:\code\intellij\cangjie\cfir\analysis-tests\build\ffi-annotation-verification\20260918-full-after-platform-annotation\cases.json"
with open(SNAP, encoding="utf-8") as f:
    cases = json.load(f)
fails = [c for c in cases if c.get("status") == "failed"]

def parse(key):
    cls, _, meth = key.partition("#")
    short = cls.split(".")[-1]
    parts = short.split("$")
    root = parts[0]
    psi = "Psi" if root.endswith("PsiTestGenerated") else "LLT"
    base = root.replace("Psi", "")
    meth = meth.replace("()", "")
    return base, psi, "$".join(parts[1:]), meth

norm = collections.defaultdict(dict)  # (base, path, method) -> {psi: key}
for c in fails:
    base, psi, path, meth = parse(c["key"])
    norm[(base, path, meth)][psi] = c

print("distinct failing test methods:", len(norm))
both = sum(1 for v in norm.values() if len(v) == 2)
print("present in both LLT & LLTPsi:", both, " only-one-path:", len(norm) - both)

print("\n== list ==")
for (base, path, meth), v in sorted(norm.items()):
    tag = "BOTH" if len(v) == 2 else "ONE "
    print(f"[{tag}] {base} : {path} #{meth}")

# failure message classification
cls_counter = collections.Counter()
for (base, path, meth), v in norm.items():
    msg = next(iter(v.values()))["message"]
    head = msg.split("\n")[0][:160]
    if "AssertionFailedError" in head:
        kind = "assert-diff"
    elif "Exception" in head:
        kind = re.sub(r"^[a-zA-Z0-9_.$]+:", "", head).strip()[:80]
        kind = head.split(":")[0]
    else:
        kind = head[:80]
    cls_counter[kind] += 1
print("\n== failure kinds ==")
for k, n in cls_counter.most_common(30):
    print(f"{n:4d}  {k}")

print("\n== top-level family counts (normalized) ==")
fam = collections.Counter()
for (base, path, meth), v in norm.items():
    top = "$".join(path.split("$")[:2])
    fam[(base, top)] += 1
for (base, top), n in sorted(fam.items(), key=lambda x: -x[1]):
    print(f"{n:4d}  {base} :: {top}")
