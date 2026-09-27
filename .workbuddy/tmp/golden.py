import glob, os, xml.etree.ElementTree as ET
# golden 文件名 -> 测试方法
mapping = {
    "testSameProjectMacroPackageDeclarationReference": "sameProjectMacroPackageDeclarationReference.macro.cfir.txt",
    "testSameProjectMacroPackageExpression": "sameProjectMacroPackageExpression.macro.cfir.txt",
}
base = r"cfir/analysis-tests/testData/macro"
for x in sorted(glob.glob(r"cfir/analysis-tests/build/test-results/test/TEST-*CfirAnalysisMacro*Generated.xml")):
    t = ET.parse(x)
    for tc in t.iter("testcase"):
        name = tc.attrib["name"].removesuffix("()")
        if name not in mapping: continue
        fail = tc.find("failure")
        if fail is None: continue
        msg = fail.attrib["message"]
        parts = msg.split("=====得到======")
        if len(parts) < 2: continue
        text = parts[1].rstrip("\n") + "\n"
        path = os.path.join(base, mapping[name])
        with open(path, "wb") as f: f.write(text.encode("utf-8"))
        print("written", mapping[name])
