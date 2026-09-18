import json, os, re, collections, datetime

SNAP = r"D:\code\intellij\cangjie\cfir\analysis-tests\build\ffi-annotation-verification\20260918-full-after-platform-annotation\cases.json"
OUT = r"D:\code\intellij\cangjie\cfir\analysis-tests\build\cfir-failing-tests-20260918.md"

with open(SNAP, encoding="utf-8") as f:
    cases = json.load(f)
fails = [c for c in cases if c.get("status") == "failed"]


def parse(key):
    cls, _, meth = key.partition("#")
    parts = cls.split(".")[-1].split("$")
    base = parts[0].replace("Psi", "")
    return base, "$".join(parts[1:]), meth.replace("()", "")


norm = {}
for c in fails:
    base, path, meth = parse(c["key"])
    norm.setdefault((base, path, meth), []).append(c["key"])

by_family = collections.defaultdict(list)
for (base, path, meth), keys in norm.items():
    by_family[path].append((meth, len(keys)))

lines = []
A = lines.append
A("# cfir 测试失败清单（工作树快照 2026-09-18）")
A("")
A("> 生成时间：2026-09-18 18:0x（本次会话实测）")
A("")
A("## 0. 结论速览")
A("")
A("- 当前工作树 **编译不过**：`:cfir:checkers:compileKotlin` 报 4 处 `Unresolved reference 'languageVersionSettings'`，")
A("  导致 `:cfir:analysis-tests:test` 等 9 个 cfir 测试任务全部被跳过。")
A("- 本次实际执行的测试任务只有 2 个：")
A("  - `:cfir:cfir-common:test` — 通过")
A("  - `:cfir:cfir-cones:test` — 18 tests，**1 failed**（`StdlibClassIdsTest > allClassIds contains all declared ClassIds()`，期望 18 实际 20）")
A("- 最后一次可用的全量基线：`20260918-full-after-platform-annotation`（今天 12:01）")
A("  - 8642 条记录 / 8111 passed / **223 failed** / 308 skipped")
A("  - 折算 **112 个不同的失败测试方法**（111 个 LLT 与 LLTPsi 两条路径同时失败，1 个仅 LightTree）")
A("  - 该基线之后又有 7 项改动只做了切片验证，全量未重跑，因此 223 是下界。")
A("")
A("## 1. 编译断裂明细（阻塞全量的直接原因）")
A("")
A("```")
A("e: cfir/checkers/.../declaration/CfirAnnotationArgNumberChecker.kt:34:67 Unresolved reference 'languageVersionSettings'")
A("e: cfir/checkers/.../declaration/CfirAnnotationTargetChecker.kt:57:61 Unresolved reference 'languageVersionSettings'")
A("e: cfir/checkers/.../declaration/CfirBuiltInAnnotationSemanticsChecker.kt:112:55 Unresolved reference 'languageVersionSettings'")
A("e: cfir/checkers/.../declaration/CfirBuiltInAnnotationSemanticsChecker.kt:127:46 Unresolved reference 'languageVersionSettings'")
A("```")
A("")
A("三处写法均为 `context.session.languageVersionSettings`，但文件里缺少")
A("`import org.cangnova.cangjie.cfir.session.languageVersionSettings`")
A("（`CheckerContext.kt:23` 有这条 import 且它内部同样写 `session.languageVersionSettings`，所以只有这三处报错）。")
A("备选写法：直接用 `context.languageVersionSettings`（`CheckerContext.kt:77` 已暴露该属性）。")
A("")
A("## 2. 全量基线失败用例（112 个，按族分组）")
A("")
A(f"共 {len(norm)} 个不同测试方法。")
A("")
for fam in sorted(by_family, key=lambda p: (-len(by_family[p]), p)):
    items = sorted(by_family[fam])
    A(f"### {fam or '(顶层宏测试)'} — {len(items)} 个")
    A("")
    for meth, n in items:
        tag = "LLT+LLTPsi" if n == 2 else "仅 LLT 或仅 PSI"
        A(f"- `{meth}` （{tag}）")
    A("")

A("## 3. 按顶层族统计")
A("")
fam_top = collections.Counter()
for (base, path, meth), keys in norm.items():
    fam_top["$".join(path.split("$")[:1]) or "(顶层)"] += 1
for k, n in fam_top.most_common():
    A(f"- {k}: {n}")
A("")
A("## 4. 报错特征归类（同一条 fixture 的 PSI/LightTree 报错一致）")
A("")
A("- `APILEVEL_REF_HIGHER` / `API_LEVEL`（含 `UNUSED_IMPORT`、`DEPRECATED_WARNING` 噪声）—— APILevelChecker 全族")
A("- `USED_BEFORE_INITIALIZATION` / `CANNOT_ASSIGN_TO_IMMUTABLE` —— InitializationCheck")
A("- `USE_MUTABLE_FUNC_ALONE` / `IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION` / `CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC` —— Record$Mut")
A("- `UNRESOLVED_REFERENCE` / `UNRESOLVED_IMPORT` / `UNUSED_IMPORT` —— ErrMsgs、Linkage、QuoteExpr、(ok_class_07 / globalfunc 环境性)")
A("- `UNREACHABLE_PATTERN` / `TYPE_MISMATCH` / `BUILTIN_INDEX_IN_BOUND` —— Assign$MultipleAssignExpr、DefaultParameterPkg02")
A("- `GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT` / `UNABLE_TO_INFER_GENERIC_FUNC` —— ConstraintCheck")
A("- `NOT_MEMBER_OF` / `NO_MATCHING_OPERATOR_INVOKE` / `WRONG_NUMBER_OF_ARGUMENTS` —— Enum、ErrMsgs")
A("- `EXTEND_DUPLICATE_INTERFACE` / `SUPER_TYPES_DUPLICATE` / `INHERITANCE_CYCLE` —— ExtendsImplementsInterfaceDuplicated")
A("- `AMBIGUOUS_USE` / `CLASSIFIER_REDECLARATION` —— Linkage$PrivateLimit")

with open(OUT, "w", encoding="utf-8") as f:
    f.write("\n".join(lines) + "\n")
print("written:", OUT, len(lines), "lines")
