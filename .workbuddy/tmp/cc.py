import glob, os, re, xml.etree.ElementTree as ET

XMLDIR = r"D:\code\intellij\cangjie\cfir\analysis-tests\build\test-results\test"
want = ["testConstraintCheckTest12", "testConstraintInstantiateTest7", "testExpose4", "testExpose5",
        "testOptionWithElement01", "testSolve1"]

seen = set()
for path in glob.glob(os.path.join(XMLDIR, "*.xml")):
    if "ConstraintCheck" not in os.path.basename(path):
        continue
    try:
        root = ET.parse(path).getroot()
    except Exception:
        continue
    for tc in root.iter("testcase"):
        name = (tc.get("name") or "").replace("()", "")
        if name not in want:
            continue
        fail = tc.find("failure")
        if fail is None:
            continue
        msg = fail.get("message") or ""
        key = (name, msg[:200])
        if key in seen:
            continue
        seen.add(key)
        print("=" * 100)
        print("CASE:", name, "|", tc.get("classname"))
        print("-" * 100)
        print(msg[:2600])
        print()
