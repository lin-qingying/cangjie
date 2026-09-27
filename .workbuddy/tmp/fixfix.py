import re
base = r"D:/code/intellij/cangjie/cfir/analysis-tests/testData/macro/llt/APILevelChecker"
def fix(path, skip, single=None):
    with open(path, "rb") as f: data = f.read().decode("utf-8")
    def repl(m):
        line = m.group(0)
        if skip in line: return line
        nl = "\r\n" if line.endswith("\r\n") else "\n"
        return "import <!UNUSED_IMPORT!>" + line[len("import "):line.rindex(nl)] + "<!>" + nl
    data = re.sub(r"import std\.[a-z][\w.]*(\.\*)?\r?\n", repl, data)
    with open(path, "wb") as f: f.write(data.encode("utf-8"))
for variant in ("level", "level_v1"):
    d = base + "\\" + variant + r"\merge_anno\merge_std\\"
    fix(d + "importall.cj", "std.argopt.*")
    import os
    if os.path.isfile(d + "importsingle.cj"):
        fix(d + "importsingle.cj", "__none__")
print("done")
