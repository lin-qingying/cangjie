import json

BASE = r"D:\code\intellij\cangjie\cfir\analysis-tests\build\ffi-annotation-verification\20260918-full-after-platform-annotation\cases.json"
with open(BASE, encoding="utf-8") as f:
    cases = json.load(f)

want = ["testConstraintCheckTest12", "testConstraintInstantiateTest7", "testExpose4", "testExpose5",
        "testOptionWithElement01", "testSolve1"]
seen = set()
for c in cases:
    if c["status"] != "failed":
        continue
    cls, _, meth = c["key"].partition("#")
    m = meth.replace("()", "")
    if m not in want:
        continue
    if "ConstraintCheck" not in cls:
        continue
    if (m, cls.split("$")[0]) in seen:
        continue
    seen.add((m, cls.split("$")[0]))
    print("=" * 100)
    print("CASE:", cls)
    print("-" * 100)
    print(c["message"][:2500])
    print()
