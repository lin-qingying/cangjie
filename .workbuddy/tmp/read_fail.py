import glob, os, xml.etree.ElementTree as ET
D = r"D:/code/intellij/cangjie/cfir/analysis-tests/build/test-results/test"
want = ["testExpose4", "testExpose5", "testConstraintInstantiateTest7", "testInterfaceProperty5", "testUpperBoundsMemberAndMethodRich"]
seen = set()
for p in glob.glob(os.path.join(D, "*.xml")):
    try: root = ET.parse(p).getroot()
    except Exception: continue
    for tc in root.iter("testcase"):
        n = (tc.get("name") or "").replace("()", "")
        if n not in want: continue
        f = tc.find("failure")
        if f is None: continue
        cls = tc.get("classname") or ""
        key = (n, cls)
        if key in seen: continue
        seen.add(key)
        print("=" * 88)
        print(f"CASE {n}  [{cls.split('.')[-1]}]")
        print((f.get("message") or "")[:1500])
