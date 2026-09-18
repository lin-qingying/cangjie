import json, sys, collections, re

SNAP = r"D:\code\intellij\cangjie\cfir\analysis-tests\build\ffi-annotation-verification\20260918-full-after-platform-annotation\cases.json"

with open(SNAP, encoding="utf-8") as f:
    cases = json.load(f)

fails = [c for c in cases if c.get("status") == "failed"]
print("total records:", len(cases), "failed:", len(fails))

def family(key):
    # key form: org.cangnova.cangjie.cfir.analysis.tests.CfirAnalysisMacroPsiTestGenerated$Llt$APILevelChecker$LevelV1$MergeAnno$MergeStd#testImportall()
    cls, _, meth = key.partition("#")
    short = cls.split(".")[-1]
    parts = short.split("$")
    root = parts[0]
    return root, parts[1:], meth

# group by (rootEntry(LLT vs Macro PSI), top-level family)
groups = collections.Counter()
testcase_groups = collections.Counter()
for c in fails:
    root, parts, meth = family(c["key"])
    path = "$".join(parts[:-1]) if len(parts) > 1 else (parts[0] if parts else "")
    # collapse: root + first two segments
    seg = parts[:3]
    groups[(root, "$".join(seg))] += 1
    if meth == "testAllFilesPresent()":
        testcase_groups[(root, "$".join(parts[:4]))] += 1

print("\n=== failures grouped by (testClass root, first 3 segments) ===")
for (root, seg), n in sorted(groups.items(), key=lambda x: (-x[1], x[0])):
    print(f"{n:5d}  {root} :: {seg}")

print("\n=== distinct failing fixtures (testAllFilesPresent = one per testData dir) ===")
tot = 0
for (root, seg), n in sorted(testcase_groups.items(), key=lambda x: (x[0])):
    print(f"{n:5d}  {root} :: {seg}")
    tot += n
print("fixture-dir failures:", tot)
