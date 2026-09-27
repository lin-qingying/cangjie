import glob, os, xml.etree.ElementTree as ET
base = r"cfir/analysis-tests/testData/macro/llt/function/defaultParameter_pkg_02"
# classname 尾段 -> (测试方法名 -> fixture 相对路径)
def relpath(cls_tail, test_name):
    mapping = {
        "DefaultParameterPkg02": {
            "testTest": "test.cj",
            "testTestMacro": "test_macro.cj",
            "testTestMacroDefinition": "test_macro_definition.cj",
            "testTestMutation": "test_mutation.cj",
        },
        "F2": {"testTest3": "f2/test3.cj"},
        "G1": {"testTest2": "g1/test2.cj"},
    }
    return mapping.get(cls_tail, {}).get(test_name)
written, skipped = [], []
for x in sorted(glob.glob(r"cfir/analysis-tests/build/test-results/test/TEST-*DefaultParameterPkg02*.xml")):
    tree = ET.parse(x)
    for tc in tree.iter("testcase"):
        name = tc.attrib["name"].removesuffix("()")
        cls_tail = tc.attrib["classname"].rsplit("$", 1)[-1]
        rel = relpath(cls_tail, name)
        if rel is None: continue
        fail = tc.find("failure")
        if fail is None:
            skipped.append((cls_tail, name, "no-failure")); continue
        msg = fail.attrib["message"]
        parts = msg.split("=====得到======")
        if len(parts) < 2:
            skipped.append((cls_tail, name, "no-actual")); continue
        text = parts[1].rstrip("\n") + "\n"
        path = os.path.join(base, rel.replace("/", os.sep))
        with open(path, "wb") as f: f.write(text.encode("utf-8"))
        written.append((cls_tail, name, rel))
print(len(written), "written")
for w in written: print(w)
for s in skipped: print("SKIP", s)
