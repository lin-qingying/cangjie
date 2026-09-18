import json, os, glob, collections, xml.etree.ElementTree as ET

BASE = r"D:\code\intellij\cangjie\cfir\analysis-tests\build\ffi-annotation-verification\20260918-full-after-platform-annotation\cases.json"
XMLDIR = r"D:\code\intellij\cangjie\cfir\analysis-tests\build\test-results\test"
OUT = r"D:\code\intellij\cangjie\cfir\analysis-tests\build\cfir-full-run-20260918-1859.md"

with open(BASE, encoding="utf-8") as f:
    base = {c["key"]: c for c in json.load(f)}

now = {}
for path in glob.glob(os.path.join(XMLDIR, "*.xml")):
    try:
        root = ET.parse(path).getroot()
    except Exception as e:
        print("skip", path, e)
        continue
    for tc in root.iter("testcase"):
        cls = tc.get("classname") or ""
        name = tc.get("name") or ""
        key = f"{cls}#{name}"
        if key in now:
            continue
        if tc.find("failure") is not None or tc.find("error") is not None:
            st = "failed"
        elif tc.find("skipped") is not None:
            st = "skipped"
        else:
            st = "passed"
        now[key] = st

base_failed = {k for k, v in base.items() if v["status"] == "failed"}
now_failed = {k for k, v in now.items() if v == "failed"}

fixed = base_failed - now_failed
regressed = now_failed - base_failed
unchanged = base_failed & now_failed


def norm(key):
    cls, _, meth = key.partition("#")
    parts = cls.split(".")[-1].split("$")
    basecls = parts[0].replace("Psi", "")
    return basecls, "$".join(parts[1:]), meth.replace("()", "")


print(f"now total={len(now)} failed={len(now_failed)}")
print(f"base failed={len(base_failed)}")
print(f"FIXED={len(fixed)} REGRESSED={len(regressed)} UNCHANGED={len(unchanged)}")

def group(keys):
    d = collections.defaultdict(list)
    for k in keys:
        b, p, m = norm(k)
        d[p or "(顶层)"].append(m)
    return d

gf = group(fixed)
gr = group(regressed)
gu = group(unchanged)

L = []
A = L.append
A("# cfir 全量测试结果（2026-09-18 18:33–18:59 实测）")
A("")
A("## 汇总")
A("")
A("| 模块 | tests | failed | 状态 |")
A("|---|---|---|---|")
A("| :cfir:analysis-tests:test | 8656 | **225** | FAILED |")
A("| :cfir:resolve:test | 5 | 0 | 通过 |")
A("| :cfir:raw-cfir:psi2cfir:test | 686 | **180** | FAILED |")
A("| :cfir:raw-cfir:light-tree2cfir:test | 247 | **9** | FAILED |")
A("| :cfir:cfir-cones:test | 18 | **1** | FAILED |")
A("| :cfir:cfir-serialization:test | — | 0 | 通过 |")
A("| :cfir:cfir-tree:test | — | 0 | 通过 |")
A("| :cfir:providers:test | — | 0 | 通过 |")
A("| :cfir:cfir-common:test | — | 0 | 通过 |")
A("| checkers / semantics / entrypoint / diagnostic-renderers | — | — | NO-SOURCE |")
A("")
A("**cfir 合计 415 条失败。**")
A("")
A(f"与 12:01 基线（223 failed / 8642）逐用例对比：**FIXED={len(fixed)}**、**REGRESSED={len(regressed)}**、UNCHANGED={len(unchanged)}。")
A("")
A("## 一、raw-cfir 两个模块（189 条，单一根因）")
A("")
A("### psi2cfir：180 failed / 686 —— 覆盖率矩阵缺项（156 条）")
A("")
A("```")
A("java.lang.IllegalStateException: rawBuilder coverage matrix misses test files: [declarations/file-structure/featuresDirective.cj]")
A("```")
A("")
A("`testData/rawBuilder/coverage-matrix.md:15` 的 `Group/declarations/file-structure` 只列了")
A("`emptyFile.cj` 与 `packageAndImport.cj`，新增的 `featuresDirective.cj` 未登记；")
A("`AbstractRawCfirBuilderTestCase.validateCoverageMatrix` 在每条 rawBuilder 用例上都会跑这个校验，")
A("因此一条漏登记放大成 156 条失败。**补一行即可清掉 156 条。**")
A("")
A("### psi2cfir + light-tree2cfir：golden 过期（24 + 9 = 33 条）")
A("")
A("| golden 文件 | psi2cfir | light-tree2cfir |")
A("|---|---|---|")
for g in ["featuresDirective.txt", "featuresDirective.lazyBodies.txt", "interfaceDeclaration.txt",
          "interfaceDeclaration.lazyBodies.txt", "packageAndImport.lazyBodies.txt", "forWithPatternGuard.txt"]:
    A(f"| {g} | ✓ | {'✓' if not g.endswith('lazyBodies.txt') else '—'} |")
A("")
A("`forWithPatternGuard.txt`、`interfaceDeclaration.txt`、`featuresDirective.txt`、`packageAndImport.lazyBodies.txt`")
A("的期望输出与当前 PSI / LightTree 两条 builder 路径的实际输出都不一致。")
A("")
A("## 二、analysis-tests：225 failed / 8656")
A("")
if regressed:
    A("### 新增失败（相对 12:01 基线）")
    A("")
    for fam in sorted(gr, key=lambda p: (-len(gr[p]), p)):
        for m in sorted(gr[fam]):
            A(f"- `{fam} #{m}`")
    A("")
if fixed:
    A("### 已修复（相对 12:01 基线）")
    A("")
    for fam in sorted(gf, key=lambda p: (-len(gf[p]), p)):
        for m in sorted(gf[fam]):
            A(f"- `{fam} #{m}`")
    A("")
A("### 仍失败（按族，112 → 现数）")
A("")
A(f"共 {len(unchanged)} 个仍在失败的用例。")
A("")
for fam in sorted(gu, key=lambda p: (-len(gu[p]), p)):
    ms = sorted(gu[fam])
    A(f"**{fam}**（{len(ms)}）")
    A("")
    for m in ms:
        A(f"- `{m}`")
    A("")

with open(OUT, "w", encoding="utf-8") as f:
    f.write("\n".join(L) + "\n")
print("written", OUT)

print("\n--- FIXED ---")
for k in sorted(fixed):
    print(" ", k)
print("\n--- REGRESSED ---")
for k in sorted(regressed):
    print(" ", k)
