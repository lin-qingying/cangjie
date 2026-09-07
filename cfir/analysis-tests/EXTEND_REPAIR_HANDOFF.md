# CFIR 扩展修复交接 — 2026-09-07

仓库：`D:\code\intellij\cangjie`。用户要求停止本轮修复，保存当前实现与日志，并合回本地 `main`，供新会话继续。第 13 项是未完成验收的检查点，不是已完成修复。

## 新会话先做什么

1. 读取本文件与 `REPAIR_LOG.md` 最后一个交接条目；应用 `cangjie-cfir-llt-repair` 技能。用户明确要求不使用子代理。
2. 先收尾新增 `testData/llt/unusedImport/ambiguous_function_targets.cj`：`explicit_no_match.cj` 分段的 `select("x")` 期望 TYPE_MISMATCH，实际 ARGUMENT_TYPE_MISMATCH，PSI/LightTree 各失败一条。官方 cjc 报 sema_mismatched_types；必须核对项目的诊断名称映射与共享调用诊断入口，再判断应修实现还是纠正新期望，不能只照 CFIR 输出改标记。
3. 定向验证第 13 项，再用固定全量命令对比 `12-after`；通过验收后单独提交完成记录。
4. 然后修 import6/import14 的消费包导入扩展冲突。后文已有官方和框架调查，避免重新从头搜索。

## 已完成与当前全量结果

此前 12 项已逐项提交并验证，最后一项为 `b9130fe2a`（数组字面量目标定型先于变参选择）。全量失败由 642 降至 612。全部已完成项的原因、官方证据及前后对比均在 REPAIR_LOG.md。

| 指标 | 第 12 项已验收基线 | 交接时最新完整报告 |
| --- | ---: | ---: |
| Gradle 测试总数 | 8436 | 8440 |
| 通过 | 7517 | 7519 |
| 失败 | 612 | 614 |
| 跳过 | 307 | 307 |
| XML 额外跳过的聚合记录 | 1 | 1 |
| XML errors | 0 | 0 |

按完整 `(suite, classname, testcase)` 键比较：既有用例 REGRESSED=0、FIXED=0；新增 4 条，operator_ambiguity_targets 两入口通过，ambiguous_function_targets 两入口失败；没有删除测试或其它状态变化。

最新 XML 的运行时间为 2026-09-07 15:45–16:00（UTC+8）。`C:\Users\lin17\.gradle\daemon\9.4.0\daemon-43052.out.log` 记录 `8440 tests completed, 614 failed, 307 skipped`；该次 JVM 为 JDK 21 / daemon 3g，与本线程基线 JDK 25 / daemon 1g 不同。交接只保存现有报告，没有再次运行测试，不能把它描述成同命令验收。

当前失败套件汇总：LLT PSI 181、LLT LightTree 179、Macro PSI 130、Macro LightTree 124。核心 Extend 为 0；ExtendImport 为 4（import6/import14 各两入口）；ExtendsImplementsInterfaceDuplicated 为 6，属于另一继承类别。

## 第 13 项已保存的实现

- `cfir/checkers/.../declaration/CfirImportsChecker.kt`：显式访问 ErrorNamedReference/ResolvedErrorReference，从普通歧义或 no-match 调用的候选读取包、扩展接口与上界使用关系；裸歧义引用没有真实目标，不消费导入。ErrorNamedReference 的默认 visitor 不经过 NamedReference，显式导入和别名需进入相同的名称记录入口。
- `cfir/resolve/.../body/CfirExpressionsResolveTransformer.kt`：`restoreInvalidSourceOperatorCall` 同时处理候选歧义与已选候选的错误结果；失败的 operator 候选不再成为导入使用目标。操作数根错误通过 ConeUnreportedDuplicateDiagnostic 传播；名称解析器已确定的该类诊断保留，不再生成额外 operator 错误。
- 新测试 `unusedImport/ambiguous_function_targets.cj`、`unusedImport/operator_ambiguity_targets.cj` 及两份 LLT tests-gen。前者覆盖星号、显式、别名、裸函数引用、失败调用和成功重载选择，当前只剩前述诊断分类差异。

已核对官方规则：普通歧义/no-match 调用消费参与候选的导入；裸歧义函数或变量引用不消费；失败的二元 operator 解糖恢复原表达式，报 INVALID_BINARY_OPERATOR，相关扩展接口仍可未使用。

官方源码入口：`external/cangjie_compiler/src/Sema/CheckUnusedImportImpl.cpp:124`、`TypeCheckExpr/BinaryExpr.cpp:843`。Kotlin 对照：`external/kotlin/analysis/analysis-api-fir/src/org/jetbrains/kotlin/analysis/api/fir/references/FirReferenceResolveHelper.kt:469`、`external/kotlin/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/body/resolve/FirExpressionsResolveTransformer.kt:641`。

中间回归已经修正并有切片证据：operator_overload/err_binary_01 只应保留无匹配 operator()；class/class_redef_14 只应保留非法泛型上界诊断。最新完整报告中这两类均没有从通过变失败。

既有失败消息的变化必须保留在对比中：import6 两入口消除了 UNUSED_IMPORT 误报，但其缺失的 shadow 检查尚未实现；NonExhaustiveEnum/BinaryCompat/change_abi、change_lib 四条记录新增 UNUSED_IMPORT。这些旧测试当前同时分析新旧库源码而形成歧义；无目标变量歧义不消费导入已有独立官方多包探针，不要用“遇到任何错误就屏蔽 unused”掩盖差异。

## 剩余扩展冲突的证据与入口

### import6：两个导入扩展的成员遮蔽

`testData/llt/Extend_import/import6/main.cj` 的 A/B/C 依赖分别通过 cjc 编译。消费包导入 B.I1、C.I2 后，官方在调用 `f` 报歧义，并在两份库中 `f` 的原声明位置分别报告 EXTEND_MEMBER_CANNOT_SHADOW。调用歧义和导入使用已正确，缺的是消费包触发的导入扩展检查。不要删除两个声明上的期望。

### import14：两个导入扩展的默认属性冲突

`testData/llt/Extend_import/import14/test.cj` 的 pkg.a/pkg.b 分别编译成功。消费包导入两个 ValueType 别名后，官方对两个 `extend Int64` 的属性 code 报 INTERFACE_MEMBER_MUST_BE_IMPLEMENTED，并报两条 UNUSED_IMPORT。

现有 fixture 的 ABSTRACT_MEMBER_NOT_IMPLEMENTED 名称及整个声明范围不正确。实现真正的检查后应按项目完整 `extend` token 范围修正；unused import 标完整 CjImportItem。不能只改期望而略过遗漏检查。

### 官方导入检查边界

- `external/cangjie_compiler/src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:302` 追加 GetAllNeedCheckExtended 后统一检查成员。
- `:1449` 按目标分组；全来自同一导入包时不重复检查；其它组收集 PUBLIC 扩展；nominal 目标需 IMPORTED 与 PUBLIC，builtin 也处理。
- `:528` 的 CollectExtendByInterfaceInherit：同包，或两者都来自其它包且在消费包可见时，按接口继承关系判断；其它情况读取扩展原声明文件的访问关系。
- `:656` 的 GetVisibleExtendMembersForExtend：跨包贡献必须 PUBLIC；按实例化与泛型映射区分可见/不可见成员；合并后删除抽象接口成员。
- `StructInheritanceChecker.h:135` 使用当前 package 的首个文件判定导入可访问性。
- `external/cangjie_compiler/src/Sema/TypeCheckExtend.cpp:409` 从所有导入包收集扩展，包括间接依赖和宏包，不以是否出现成员调用为条件。
- import7_1 官方只报告本地扩展，不能假设本地与导入扩展对称。PUBLIC 属性在导出/加载阶段的完整来源仍需追踪，未形成最终结论。

### CFIR 框架注意点

- 复用 `CfirExtendExtraChecker.checkMemberShadowing` 与 `CfirInheritanceDeepChecker.checkDefaultInterfaceMemberConflicts`；两者当前 private，尚未实现导入消费包入口。
- `hasConcreteImplementation` 第一轮直接遍历 targetScope，没有先走 accessibility/provenance；不能让不参与导入检查的本地实现消除真实冲突。
- `CfirClassUseSiteMemberScope` 有 USE_SITE/BODY_LOOKUP/DECLARATION_SITE 和 excludingExtend，没有导入扩展集合视图。可缓存的结构 scope 不应读取临时全局文件状态。
- `CfirAccessibilityChecker.checkExtend/isExtendExported` 已统一目标、接口、泛型上界的导出规则；通用 status.visibility 不等价于扩展是否导出。
- `CfirExtendProvider` 尚无全部扩展查询；source 索引已有 allModels，库 provider 已有 byPackage/byDeclarationPackage。若扩充接口，要同步 composite、lazy composer、empty provider 和测试实现。
- 反序列化扩展没有源码 CfirFile/source。不能只解决同一个测试 Session 内的多包源码，而遗漏真实库路径。
- PendingDiagnosticsReporterImpl 提交时断言文件路径一致。跨文件诊断必须正确处理文件归属、suppression、去重与提交，不能将库声明 offsets 放到消费文件名下。
- Kotlin 参考：FirExtensionShadowedByMemberChecker、FirConflictsDeclarationChecker、PendingDiagnosticsReporterImpl。Kotlin 的 Library conflict 分支跳过；仓颉的导入扩展复查是有官方证据的语义差异。

数值字面量后缀另有未修复问题：`1i8` 不能按 Int64 定型，Float32/Float64 显式后缀同理。当前 transformLiteralExpression 会丢失后缀。证据为 `evidence/numeric_suffix_context.official.cjc.json`；第 12 项不代表这个问题已完成。

## 验证命令与本地证据

固定全量命令（不先跑 build/assemble）：

```powershell
.\gradlew-queue.bat :cfir:analysis-tests:test --no-daemon --max-workers=1 --console=plain '-Dorg.gradle.jvmargs=-Xmx1g'
```

定向测试使用完整模式，例如 `--tests 'org.cangnova.cangjie.cfir.analysis.tests.*UnusedImport*'`。需要编译时可加 `'-Pkotlin.compiler.execution.strategy=in-process'`；编译后的 Gradle 进程可能与测试争抢提交内存，中断属于基础设施问题。

不要使用短 `*Default*`：Windows Java launcher 曾把它扩展为根目录的 default.cjo。保留该文件，用完整类名前缀解决。

证据根目录：`D:\code\intellij\cangjie\cfir\analysis-tests\build\repair-20260906`（本机 build 产物，不在 Git 中；新会话先使用，避免 clean）。

- `12-after`：最后完成验收的全量基线。
- `13-full-check`：中间全量，存在已修正的 ClassRedef14 回归，不作当前最终结果。
- `13-family-final`、`13-root-diagnostics`：中间切片。最后本线程扩大切片为 1111 项、59 失败、64 跳过，随后现有完整报告已保存如下。
- `13-handoff-latest`、`12-after--13-handoff-latest.json`：本次交接的完整 XML 与逐键对比。
- `package-evidence/ambiguous-function-targets-expanded`、`explicit-function-targets`、`operator-ambiguity-import-targets`、`ambiguous-variable-targets`、`import6`、`import14`：官方源码、逐包命令和原始诊断。
- `repair_results.py` 支持 snapshot/compare/compare-slice/diffs；snapshot 拒绝覆盖现有目录。
- `capture_cjc.py`、`capture_cjc_packages.py`：去除标记后按官方编译器取证；多包数据必须分别编译依赖。
- 可用 Python：`C:\Users\lin17\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe`，加 `-X utf8`。PATH 的 WindowsApps Python 不可用。

## 原有未提交工作必须保留

此交接只提交第 13 项的两个生产文件、两个新 fixture、两份 LLT 生成入口和本轮日志。其它工作区变化仍保留，不能 reset、全量 add 或混入本修复：

- REPAIR_LOG.md 原有大段未提交内容和空白差异。
- Extend/Extend_Refactor/extend_mutable_function_invalid_1.cj。
- CfirDeclDeserializer.kt、DeprecationsProvider.kt、CommonTypeCheckers.kt、CfirDeprecatedCallChecker.kt、未跟踪的 CfirDeprecatedTypeRefChecker.kt。
- CfirExtendExtraChecker.kt 的 primitive shadow 工作；CfirInheritanceDeepChecker.kt 的返回类型去重工作。
- Macro 生成类的原有行尾变化、.idea/workspace.xml、.agents/probes/、根目录 tmp_*.txt/capture_lines.txt 等。

仍按每个独立问题完成验证后提交；使用 apply_patch 写文件、PowerShell 7、UTF-8 读取。已有文档校验另有 `docs/module-catalog.md` 漏列 :gradle-queue-cli 的问题，本轮未处理。
